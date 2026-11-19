import Foundation

/// 앱 최상위 목적지 — Android `RunVealRoute.Main` / `RunVealRoute.Home`.
enum RunvealAppRoute: Hashable {
    case splash
    /// 첫 실행에 한 번만 — 끝내거나 건너뛰면 "봤다"로 적히고 다음부터는 스플래시가 바로 홈으로 보낸다.
    case onboarding
    case home
}

/// 하단 탭 4개. Android는 현재 목적지에서 탭 인덱스를 역산하지만, iOS는 선택된 탭을 그대로 들고 있는다
/// (`RunvealHomeNavigator.selectedTab`).
enum RunvealHomeTab: Int, Hashable, CaseIterable {
    case home = 0
    case course = 1
    case record = 2
    case my = 3
}

/// 홈 탭 위에 쌓이는 지도 흐름 목적지 — Android `RunVealRoute.HomeTab` 중 지도를 유지한 채
/// 탭바를 숨기고 올라가는 것들.
///
/// 탭 첫 화면(홈 지도 / 내 코스 / 기록 / MY)은 [RunvealHomeTab]이 들고 있으므로 여기에는 없다 —
/// Android는 탭 첫 화면과 오버레이 흐름을 한 NavHost에 같이 두고 `hasRoute`로 탭바 노출을
/// 판단하는데, iOS는 "탭 + 그 위에 쌓인 스택"으로 나누는 게 NavigationStack 모델과 맞는다.
enum RunvealHomeOverlayRoute: Hashable {
    /// 지도를 유지한 채 탭바를 숨기고 올라가는 코스 추천 흐름.
    case courseRecommend
    /// 코스 추천 결과 화면 — `POST /api/v1/routes` 응답(0~3개)을 보여준다.
    case courseRecommendResult
    /// 코스 추천 결과에서 카드를 눌러 들어가는 코스 상세.
    case courseDetail(routeId: String)
    /// 지도를 유지한 채 탭바를 숨기고 올라가는 러닝 기록 시작/종료 흐름.
    case recording
    /// 기록 종료 후 러닝 결과 화면 — 배경이 불투명이라 지도는 안 보이지만, 탭바 숨김 흐름은
    /// [recording]/[courseRecommend]와 같다.
    case result
    /// MY 탭에서 들어가는 약관 한 건. 탭 안에서 열지만 그 자체로 전체 화면이라 탭바를 숨긴다 —
    /// Android도 `TermsDetail`을 탭바 숨김 흐름(`isTermsFlow`)에 넣는다.
    case termsDetail(item: TermsPolicyItem)
}
