import Foundation
import SharedLogic

enum CourseRecommendResultIntent: UiIntent {
    case selectRoute(routeId: String)

    /// 상단 조건 칩 — 코스 조건 설정 시트를 지금 조건 그대로 띄운다.
    case openFilterSheet
    /// 시트를 닫는다(뒤로가기 포함) — 고치던 값은 버린다.
    case closeFilterSheet
    /// 시트에서 값을 하나 바꿨다. 적용 전까지는 초안에만 쌓인다.
    case updateFilterDraft(CourseRecommendSettings)
    /// 시트의 "초기화" — 코스 만들기와 같은 기본값으로 되돌린다(출발·도착지는 유지).
    case resetFilterDraft
    /// "코스 조건 적용하기" — 바꾼 조건으로 다시 추천받는다.
    case applyFilterDraft
    /// 결과 없음 화면의 "순환 코스로 찾기" — 도착지를 비워(출발지로 돌아오는 LOOP) 바로 다시 받는다.
    case retryAsLoopCourse
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
    /// 이 결과를 만든 B0 설정값 — 앱바 아래 칩으로 보여주고, 조건을 바꿔 다시 받을 때 바탕이 된다.
    var settings: CourseRecommendSettings
    /// 조건 설정 시트에서 고치고 있는 값 — nil이면 시트가 닫혀 있다. 적용할 때까지 [settings]는
    /// 그대로 둬서, 시트를 닫으면 바꾸던 값이 버려진다.
    var filterDraft: CourseRecommendSettings? = nil
    /// 바꾼 조건으로 다시 받는 중 — 조건 칩과 지도는 그대로 두고 로딩만 덮는다(화면이 비면
    /// 뒤로 간 것처럼 보인다).
    var isReloading: Bool = false

    var selectedRoute: RouteCardUiModel? {
        routes.first { $0.routeId == selectedRouteId }
    }
}
