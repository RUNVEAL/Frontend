import Foundation
import SharedLogic

/// Android `CourseCreateViewModel`에 해당한다.
@MainActor
final class CourseCreateViewModel: MviViewModel<CourseCreateIntent, CourseCreateState, CourseCreateEffect> {
    /// TEMP — 출발지를 아직 안 골랐고 현재 위치도 못 받았을 때 쓰는 최후 기본 좌표(성수동 인근).
    private static let tempStartLocation = LatLon(lat: 37.5445, lon: 127.0557)

    /// TODO: 임시 로딩 — 디자인 적용된 로딩으로 교체되면 이 최소 노출 시간도 다시 검토.
    /// 응답이 이것보다 빨리 와도 로딩 모션이 최소 한 번은 눈에 보이게 그만큼은 붙잡아 둔다.
    private static let minimumLoadingDuration: Duration = .milliseconds(900)

    /// 진행 중인 행정동 조회 — 지도를 연달아 탭하면 앞선 요청을 버린다.
    private var placeNameTask: Task<Void, Never>?

    init() {
        super.init(initialState: CourseCreateState())
    }

    override func handleIntent(_ intent: CourseCreateIntent) async {
        switch intent {
        case .startLocationClicked:
            setState { var s = $0; s.locationSelectionTarget = .start; return s }

        case .endLocationClicked:
            setState { var s = $0; s.locationSelectionTarget = .end; return s }

        case .cancelLocationSelection:
            placeNameTask?.cancel()
            setState { state in
                var next = state
                next.locationSelectionTarget = .none
                next.pickedLocation = nil
                next.pickedPlaceName = nil
                return next
            }

        case .confirmLocationSelection:
            setState { state in
                var next = state
                switch state.locationSelectionTarget {
                case .start:
                    next.startLocation = state.selectedPlaceName
                    next.startLatLon = state.pickedLocation ?? state.startLatLon
                case .end:
                    next.endLocation = state.selectedPlaceName
                    next.endLocationIsDefault = false
                    next.endLatLon = state.pickedLocation ?? state.endLatLon
                case .none:
                    return state
                }
                next.locationSelectionTarget = .none
                next.pickedLocation = nil
                next.pickedPlaceName = nil
                return next
            }

        case .mapLocationPicked(let location):
            // 옛 이름을 지우고 넣는다 — 안 지우면 새로 찍은 자리에 먼 동 이름이 잠깐 남는다.
            setState { state in
                var next = state
                next.pickedLocation = location
                next.pickedPlaceName = nil
                return next
            }
            lookUpPlaceName(location)

        case .openFilterSheet:
            setState { var s = $0; s.showFilterSheet = true; return s }

        case .closeFilterSheet:
            setState { var s = $0; s.showFilterSheet = false; return s }

        case .selectPresetDistance(let distance):
            setState { var s = $0; s.selectedDistance = distance; s.isCustomDistanceSelected = false; return s }

        case .selectCustomDistanceChip:
            setState { var s = $0; s.isCustomDistanceSelected = true; return s }

        case .changeCustomDistanceIndex(let index):
            setState { var s = $0; s.customDistanceIndex = index; return s }

        case .setAvoidVisitedPreferred(let preferred):
            setState { var s = $0; s.avoidVisitedPreferred = preferred; return s }

        case .toggleFilter(let filter):
            setState { state in
                var next = state
                if next.selectedFilters.contains(filter) {
                    next.selectedFilters.remove(filter)
                } else {
                    next.selectedFilters.insert(filter)
                }
                return next
            }

        case .resetFilterSheet:
            setState { state in
                var next = state
                next.selectedDistance = CourseCreateOptions.defaultDistance
                next.isCustomDistanceSelected = false
                next.customDistanceIndex = CourseCreateOptions.defaultCustomDistanceIndex
                next.avoidVisitedPreferred = true
                next.selectedFilters = []
                return next
            }

        case .applyFilterSheet:
            setState { var s = $0; s.showFilterSheet = false; return s }

        case .recommendClicked(let currentLocation):
            await recommendRoutes(currentLocation: currentLocation)
        }
    }

    /// 탭한 자리의 행정동 이름을 받아 온다(`IosPlaceName` → `GET /api/v1/dong`). 못 받으면 상태를
    /// 건드리지 않아서 화면은 좌표를 그대로 보여준다 — 위치를 고르는 일 자체는 이름 없이도 되므로
    /// 오류로 다루지 않는다.
    ///
    /// 지도를 연달아 탭하면 앞선 요청을 취소하고, 그래도 늦게 도착한 응답은 좌표가 그대로인지
    /// 확인한 뒤에만 반영한다 — 취소가 응답 처리 직전에 걸리면 엉뚱한 자리의 이름이 박힌다.
    private func lookUpPlaceName(_ location: LatLon) {
        placeNameTask?.cancel()
        placeNameTask = Task { [weak self] in
            guard let name = try? await IosPlaceName.shared.fetch(lat: location.lat, lon: location.lon),
                  !Task.isCancelled
            else { return }
            self?.setState { state in
                var next = state
                // 그 사이 다른 자리를 찍었으면 버린다.
                if next.pickedLocation?.lat == location.lat, next.pickedLocation?.lon == location.lon {
                    next.pickedPlaceName = name
                }
                return next
            }
        }
    }

    /// `docs/api/API.md` §3.3 — B0 설정값으로 `/api/v1/routes`를 호출하고, 성공·실패 모두 홀더에
    /// 담은 뒤 결과 화면으로 넘어간다.
    ///
    /// 출발지를 한 번도 안 골랐으면 화면이 미리 받아 둔 [currentLocation]을 쓰고, 그마저도 못 받았으면
    /// (권한 없음·위치 서비스 꺼짐 등) 임시 기본 좌표로 떨어진다.
    private func recommendRoutes(currentLocation: LatLon?) async {
        let startedAt = Date()
        // 출발지를 안 골랐으면 이번에 실제로 쓴 현재 위치를 박아 둔다 — 안 그러면 결과 화면에서
        // 조건만 바꿔 다시 받을 때 출발지가 비어 임시 좌표로 떨어진다(Android와 같은 이유).
        let settings = CourseRecommendSettings(
            selectedDistance: state.selectedDistance,
            isCustomDistanceSelected: state.isCustomDistanceSelected,
            customDistanceIndex: state.customDistanceIndex,
            avoidVisitedPreferred: state.avoidVisitedPreferred,
            selectedFilters: state.selectedFilters,
            startLatLon: state.startLatLon ?? currentLocation ?? Self.tempStartLocation,
            endLatLon: state.endLatLon
        )

        do {
            let response = try await IosCourseRecommend.shared.fetchRoutes(
                start: settings.startLatLon ?? Self.tempStartLocation,
                end: settings.endLatLon,
                distanceMeters: Int32(settings.distanceMeters),
                preferUnexplored: settings.avoidVisitedPreferred
            )
            CourseRecommendResultHolder.shared.publish(.success(response: response, settings: settings))
        } catch {
            CourseRecommendResultHolder.shared.publish(
                .failure(
                    message: error.localizedDescription.isEmpty
                        ? CourseRecommendStrings.recommendFailed
                        : error.localizedDescription,
                    settings: settings
                )
            )
        }

        let elapsed = Date().timeIntervalSince(startedAt)
        let minimum = 0.9
        if elapsed < minimum {
            try? await Task.sleep(for: .seconds(minimum - elapsed))
        }
        sendEffect(.navigateToResult)
    }
}
