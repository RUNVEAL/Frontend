import Combine
import Foundation
import SharedLogic

/// 지도 위에 뜨는 결과라 Effect는 필요 없다 — Android도 `Nothing`.
@MainActor
final class CourseRecommendResultViewModel: MviViewModel<
    CourseRecommendResultIntent,
    CourseRecommendResultScreenState,
    NoEffect
> {
    private var cancellable: AnyCancellable?

    init() {
        super.init(initialState: .loading)
        cancellable = CourseRecommendResultHolder.shared.$result
            .sink { [weak self] result in
                guard let self else { return }
                self.setState { _ in
                    guard let result else { return .loading }
                    return .ready(Self.ready(from: result))
                }
            }
    }

    override func handleIntent(_ intent: CourseRecommendResultIntent) async {
        switch intent {
        case .selectRoute(let routeId):
            updateReady { $0.selectedRouteId = routeId }

        case .openFilterSheet:
            updateReady { $0.filterDraft = $0.settings }

        case .closeFilterSheet:
            updateReady { $0.filterDraft = nil }

        case .updateFilterDraft(let draft):
            updateReady { $0.filterDraft = draft }

        case .resetFilterDraft:
            // 출발·도착지는 코스 만들기에서 고른 값이라 그대로 두고, 고를 수 있는 조건만 되돌린다.
            updateReady { $0.filterDraft = ($0.filterDraft ?? $0.settings).resetToDefaults() }

        case .applyFilterDraft:
            guard case .ready(let ready) = state, let draft = ready.filterDraft else { return }
            await recommend(with: draft)

        case .retryAsLoopCourse:
            // 도착지만 비우고 나머지 조건은 그대로 — 안내 줄이 말하는 "출발지로 돌아오는 코스"다.
            guard case .ready(let ready) = state else { return }
            await recommend(with: ready.settings.with(clearEnd: true))
        }
    }

    /**
     주어진 조건으로 다시 추천받는다. 결과는 홀더에 publish하고 **상태도 직접 갈아 끼운다** —
     조건만 바꾸고 결과가 똑같이 오면 `@Published`가 같은 값이라 보고 흘릴 수 있어서, 위 구독만
     믿으면 로딩이 영영 안 걷힌다(Android도 같은 이유로 둘 다 한다).

     다시 받는 동안에도 조건 칩과 지도는 그대로 두고 로딩만 덮는다.
     */
    private func recommend(with settings: CourseRecommendSettings) async {
        setState { state in
            guard case .ready(var ready) = state else { return state }
            ready.filterDraft = nil
            ready.isReloading = true
            return .ready(ready)
        }

        let result: CourseRecommendResult
        do {
            let response = try await IosCourseRecommend.shared.fetchRoutes(
                start: settings.startLatLon ?? Self.fallbackStart,
                end: settings.endLatLon,
                distanceMeters: Int32(settings.distanceMeters),
                preferUnexplored: settings.avoidVisitedPreferred
            )
            RunvealAnalyticsLogger.logCourseRecommend(resultCount: response.routes.count)
            result = .success(response: response, settings: settings)
        } catch {
            RunvealAnalyticsLogger.logCourseRecommend(resultCount: nil)
            result = .failure(
                message: error.localizedDescription.isEmpty
                    ? CourseRecommendStrings.recommendFailed
                    : error.localizedDescription,
                settings: settings
            )
        }
        // 홀더에도 올려 둔다 — 코스 상세가 여기서 routeId로 코스를 찾는다.
        CourseRecommendResultHolder.shared.publish(result)
        setState { _ in .ready(Self.ready(from: result)) }
    }

    /// 출발지가 비는 일은 없어야 하지만(코스 만들기에서 박아 둔다), 혹시 비면 추천을 아예 못 하는
    /// 대신 성수동 인근을 쓴다 — `CourseCreateViewModel`의 임시 기본 좌표와 같다.
    private static let fallbackStart = LatLon(lat: 37.5445, lon: 127.0557)

    private func updateReady(_ block: (inout CourseRecommendResultReady) -> Void) {
        setState { state in
            guard case .ready(var ready) = state else { return state }
            block(&ready)
            return .ready(ready)
        }
    }

    private static func ready(from result: CourseRecommendResult) -> CourseRecommendResultReady {
        switch result {
        case .success(let response, let settings):
            let routes = response.routes.enumerated().map { index, route in
                card(from: route, index: index)
            }
            return CourseRecommendResultReady(
                routes: routes,
                emptyMessage: routes.isEmpty
                    ? emptyMessage(warnings: response.warnings, shortestDistanceM: response.shortestDistanceM)
                    : nil,
                selectedRouteId: routes.first?.routeId,
                settings: settings
            )
        case .failure(let message, let settings):
            return CourseRecommendResultReady(
                routes: [],
                emptyMessage: message,
                selectedRouteId: nil,
                settings: settings
            )
        }
    }

    private static func card(from route: RouteDto, index: Int) -> RouteCardUiModel {
        let elevation = route.elevationGainM
        let newRoad = route.newRoadM
        return RouteCardUiModel(
            routeId: route.routeId,
            courseLabel: courseLabel(index: index),
            tags: Array(route.tags.prefix(2)),
            distanceValue: String(format: "%.1f", Double(route.distanceM) / 1000),
            distanceUnit: "km",
            timeValue: "\(route.durationSec / 60)",
            timeUnit: "분",
            // 값이 없으면 "-" 같은 대체 텍스트 없이 그냥 공란 — 카드가 같은 너비로 칸을 맞춘다.
            elevationValue: elevation.map { "+\($0.intValue)" } ?? "",
            elevationUnit: elevation != nil ? "m" : "",
            newRoadValue: newRoad.map { String(format: "%.1f", Double(truncating: $0) / 1000) } ?? "",
            newRoadUnit: newRoad != nil ? "km" : "",
            path: IosCourseRecommend.shared.pathOf(route: route),
            isLoop: route.type == "LOOP"
        )
    }

    /// "코스 A", "코스 B" … 라벨의 글자 부분.
    static func courseLabel(index: Int) -> String {
        guard let scalar = UnicodeScalar(UInt8(65 + min(max(index, 0), 25))) as UnicodeScalar? else { return "A" }
        return String(Character(scalar))
    }

    /// §6.3 "결과 0개" 케이스 — `warnings`로 안내 문구를 고른다.
    private static func emptyMessage(warnings: [String], shortestDistanceM: KotlinLong?) -> String {
        if warnings.contains("DESTINATION_FARTHER_THAN_DISTANCE"), let shortestDistanceM {
            let km = String(format: "%.1f", Double(truncating: shortestDistanceM) / 1000)
            return CourseRecommendStrings.resultEmptyTooFar(km)
        }
        if warnings.contains("NO_ROUTE_FOUND") {
            return CourseRecommendStrings.resultEmptyNoRoute
        }
        return CourseRecommendStrings.resultEmptyDefault
    }
}
