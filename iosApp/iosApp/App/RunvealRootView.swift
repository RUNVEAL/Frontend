import SwiftUI

/// 앱 최상위 — 스플래시 → 홈. Android `App.kt`의 최상위 `NavHost`에 해당한다.
///
/// 스플래시는 뒤로 돌아갈 곳이 아니라서 스택을 쌓지 않고 통째로 갈아 끼운다.
struct RunvealRootView: View {
    @StateObject private var navigator = RunvealAppNavigator()

    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            switch navigator.route {
            case .splash:
                SplashScreen { navigator.replace(with: .home) }
            case .home:
                RunvealHomeShell()
            }
        }
        // 다크 모드 단일 테마 — Info.plist의 `UIUserInterfaceStyle`과 함께 시스템 설정과 무관하게 고정.
        .preferredColorScheme(.dark)
    }
}
