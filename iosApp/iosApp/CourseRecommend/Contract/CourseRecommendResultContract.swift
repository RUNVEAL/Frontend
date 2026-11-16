import Foundation
import SharedLogic

enum CourseRecommendResultIntent: UiIntent {
    case selectRoute(routeId: String)
}

/// `RouteDto` 하나를 [RunvealCourseCard]가 그대로 쓸 수 있게 포맷한 모델.
struct RouteCardUiModel: Identifiable, Equatable {
    var id: String { routeId }
    let routeId: String
    /// "A", "B" … 라벨의 글자 부분. 전체 문구는 화면에서 조합한다.
    let courseLabel: String
    let tags: [String]
    let distanceValue: String
    let distanceUnit: String
    let timeValue: String
    let timeUnit: String
    let elevationValue: String
    let elevationUnit: String
    let newRoadValue: String
    let newRoadUnit: String
    /// 지도에 선을 그리고 카메라를 맞추는 데 쓴다.
    let path: [LatLon]
    /// `RouteDto.type == "LOOP"` — 출발/도착이 같다. 지도에 도착 마커를 따로 안 띄울 때 쓴다.
    let isLoop: Bool

    static func == (lhs: RouteCardUiModel, rhs: RouteCardUiModel) -> Bool {
        lhs.routeId == rhs.routeId
    }
}

enum CourseRecommendResultScreenState: UiState {
    case loading
    case ready(CourseRecommendResultReady)
}

struct CourseRecommendResultReady {
    let routes: [RouteCardUiModel]
    /// 코스가 0개일 때 보여줄 안내 문구(§6.3) — 코스가 있으면 nil.
    let emptyMessage: String?
    /// 지도 위 코스 리스트에서 선택된 카드. 코스가 있으면 첫 코스가 기본 선택.
    var selectedRouteId: String?
    /// 이 결과를 만든 B0 설정값 — 앱바 아래 칩으로 그대로 보여준다.
    let settings: CourseRecommendSettings

    var selectedRoute: RouteCardUiModel? {
        routes.first { $0.routeId == selectedRouteId }
    }
}
