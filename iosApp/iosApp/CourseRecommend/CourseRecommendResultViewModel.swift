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
            setState { state in
                guard case .ready(var ready) = state else { return state }
                ready.selectedRouteId = routeId
                return .ready(ready)
            }
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
