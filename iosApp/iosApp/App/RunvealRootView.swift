import SwiftUI

/// 앱 최상위 — 스플래시 → 홈. Android `App.kt`의 최상위 `NavHost`에 해당한다.
///
/// 스플래시는 뒤로 돌아갈 곳이 아니라서 스택을 쌓지 않고 통째로 갈아 끼운다.
struct RunvealRootView: View {
    @StateObject private var navigator = RunvealAppNavigator()
    @StateObject private var networkState = RunvealNetworkState()

    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            switch navigator.route {
            case .splash:
                SplashScreen { destination in navigator.replace(with: destination) }
            case .onboarding:
                OnboardingScreen { navigator.replace(with: .home) }
            case .home:
                RunvealHomeShell()
            }

            // 어느 화면에 있든 네트워크가 끊기면 그 위를 덮는다. 화면마다 붙이지 않고 여기 한 번만
            // 둔다 — 앱바의 뒤로가기로 닫으면 원래 화면으로 돌아간다(NetworkErrorOverlay 주석).
            NetworkErrorOverlay(state: networkState)
        }
        // 임시 진단 패널 — **스플래시가 아니라 여기 둔다.** 스플래시에 붙이면 홈으로 넘어가는
        // 순간 같이 사라져서, 정작 보고 싶은 "전환 이후"를 못 본다. 원인을 찾으면 지운다
        // (RunvealTrace.swift).
        .overlay(alignment: .bottom) { RunvealTraceOverlay() }
        // 크래시 핸들러를 **한 번 더** 깐다. `iOSApp.init`에서 깔아도 Crashlytics가 자기 핸들러를
        // 나중에 설치하면서 덮어쓰는지, 죽어도 `[치명]` 줄이 남지 않았다 — 화면이 뜬 뒤 다시 걸어
        // 이쪽이 마지막이 되게 한다.
        .task { RunvealTrace.installCrashHandlers() }
        // 다크 모드 단일 테마 — Info.plist의 `UIUserInterfaceStyle`과 함께 시스템 설정과 무관하게 고정.
        .preferredColorScheme(.dark)
    }
}
