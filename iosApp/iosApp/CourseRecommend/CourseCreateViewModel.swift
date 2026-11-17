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
            setState { var s = $0; s.locationSelectionTarget = .none; s.pickedLocation = nil; return s }

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
                return next
            }

        case .mapLocationPicked(let location):
            setState { var s = $0; s.pickedLocation = location; return s }

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

    /// `docs/api/API.md` §3.3 — B0 설정값으로 `/api/v1/routes`를 호출하고, 성공·실패 모두 홀더에
    /// 담은 뒤 결과 화면으로 넘어간다.
    ///
    /// 출발지를 한 번도 안 골랐으면 화면이 미리 받아 둔 [currentLocation]을 쓰고, 그마저도 못 받았으면
    /// (권한 없음·위치 서비스 꺼짐 등) 임시 기본 좌표로 떨어진다.
    private func recommendRoutes(currentLocation: LatLon?) async {
        let startedAt = Date()
        let settings = CourseRecommendSettings(
            distanceLabel: state.distanceLabel,
            avoidVisitedPreferred: state.avoidVisitedPreferred,
            selectedFilterCount: state.selectedFilters.count
        )

        do {
            let response = try await IosCourseRecommend.shared.fetchRoutes(
                start: state.startLatLon ?? currentLocation ?? Self.tempStartLocation,
                end: state.endLatLon,
                distanceMeters: Int32(Self.distanceMeters(from: state.distanceLabel)),
                preferUnexplored: state.avoidVisitedPreferred
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

    /// "5km"/"12.5km" → 5000/12500. 프리셋·커스텀 값 전부 0.5km 단위라 서버의 500m 단위 제약과 맞는다.
    private static func distanceMeters(from label: String) -> Int {
        let numeric = label.replacingOccurrences(of: "km", with: "")
        return Int((Double(numeric) ?? 5) * 1000)
    }
}
