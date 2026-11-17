import SwiftUI

/// 앱 최상위 내비게이션(스플래시 → 홈) — Android `App.kt`의 최상위 `NavHost` 자리.
@MainActor
final class RunvealAppNavigator: ObservableObject {
    @Published private(set) var route: RunvealAppRoute = .splash

    /// 스플래시는 뒤로 돌아갈 곳이 아니라서 스택을 쌓지 않고 통째로 갈아 끼운다
    /// (Android: `popUpTo<Splash> { inclusive = true }`).
    func replace(with route: RunvealAppRoute) {
        self.route = route
    }
}

/// 홈 셸(Map Shell)의 내비게이션 — 선택된 탭과, 그 위에 쌓인 지도 흐름 스택을 들고 있다.
/// Android `HomeScreen`의 탭 NavHost + 중첩 오버레이 그래프에 대응한다.
@MainActor
final class RunvealHomeNavigator: ObservableObject {
    @Published var selectedTab: RunvealHomeTab = .home

    /// 홈 탭 위에 쌓인 오버레이 흐름. 비어 있지 않으면 탭바를 숨긴다.
    @Published private(set) var overlayPath: [RunvealHomeOverlayRoute] = []

    /// 오버레이 흐름(코스 추천/러닝 기록/결과)에 들어가 있는가 — 탭바 노출 판단.
    var isOverlayFlowActive: Bool { !overlayPath.isEmpty }

    /// 지금 보여야 하는 오버레이 목적지(스택 맨 위). 없으면 탭 화면을 그린다.
    var currentOverlay: RunvealHomeOverlayRoute? { overlayPath.last }

    func push(_ route: RunvealHomeOverlayRoute) {
        overlayPath.append(route)
    }

    func pop() {
        guard !overlayPath.isEmpty else { return }
        overlayPath.removeLast()
    }

    /// 오버레이 흐름을 통째로 닫고 탭 화면으로 돌아간다 — Android `popUpTo(HomeMap)`.
    func popToTab() {
        overlayPath.removeAll()
    }

    /// 스택을 비우고 [route] 하나만 남긴다.
    ///
    /// - 코스 상세에서 러닝을 시작할 때: 코스 추천 흐름을 전부 지우고 들어간다 — 안 지우면 러닝
    ///   종료 후 뒤로가기로 홈이 아니라 코스 추천 화면으로 돌아간다.
    /// - 러닝 종료 후 결과로 갈 때: 기록 화면을 스택에서 빼야 결과에서 뒤로 가면 홈이 된다.
    func replaceStack(with route: RunvealHomeOverlayRoute) {
        overlayPath = [route]
    }

    func select(tab: RunvealHomeTab) {
        // 탭을 바꾸면 지도 흐름은 끝난 것으로 본다(Android도 탭 전환 시 오버레이 그래프를 벗어난다).
        overlayPath.removeAll()
        selectedTab = tab
    }
}
