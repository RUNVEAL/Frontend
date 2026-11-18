import SharedLogic
import SwiftUI

enum SplashStrings {
    static let tagline = "달릴수록 채워지는\n우리 동네 지도"
}

/// 스플래시 — Android `SplashGraph` + `SplashScreen`에 해당한다.
///
/// 홈 지도의 동/시군구/시도 오버레이를 여기서 미리 만든다 — 전국 경계 GeoJSON 파싱과 동별 탐험률
/// 집계에 몇 초가 걸려서, 홈에 들어간 뒤에 시작하면 빈 지도를 보다가 뒤늦게 동이 떠오른다.
/// `/meta`와 **같이** 시작한다 — 서버가 죽어 있으면 `/meta`가 타임아웃까지 10초를 쓰는데, 그 뒤에
/// 시작하면 스플래시가 그만큼 더 길어진다.
struct SplashScreen: View {
    let onFinished: () -> Void

    /// `/meta`가 실패(오프라인·서버 꺼짐)하면 거의 즉시 지나가 버리니 그때만 채우는 최소 노출 시간.
    private static let minimumVisibleDuration: Duration = .seconds(2)

    /// 동 오버레이 사전 생성을 기다려 주는 상한. 넘기면 그냥 홈으로 보내고, 다 만들어지는 대로
    /// 홀더를 통해 지도에 얹힌다 — 동이 늦게 뜨는 건 참을 만하지만 스플래시에 오래 잡아 두는 건 아니다.
    private static let overlayWarmUpTimeout: Duration = .seconds(8)

    var body: some View {
        VStack(spacing: RunvealSpacing.space24) {
            RunvealImages.splash
                .resizable()
                .scaledToFit()
                .frame(width: 210, height: 140)
            Text(SplashStrings.tagline)
                .runvealTextStyle(RunvealTheme.typography.heading2)
                .foregroundStyle(RunvealTheme.colors.text.primary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, RunvealLayout.marginScreen)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(RunvealTheme.colors.bg.canvas)
        .task { await bootstrap() }
    }

    private func bootstrap() async {
        let overlayWarmUp = Task { await warmUpDongOverlay() }
        let metaSucceeded = await fetchMeta()

        if !metaSucceeded {
            try? await Task.sleep(for: Self.minimumVisibleDuration)
        }
        // 오버레이가 준비될 때까지(상한까지) 기다렸다 홈으로 넘어간다.
        _ = await withTaskGroup(of: Void.self) { group in
            group.addTask { _ = await overlayWarmUp.value }
            group.addTask { try? await Task.sleep(for: Self.overlayWarmUpTimeout) }
            await group.next()
            group.cancelAll()
        }

        // TODO: 로그인 여부 확인 후 로그인 화면/홈으로 분기. 로그인 기능 구현 전까지는 항상 홈으로 이동
        //  (Android도 같은 TODO 상태다).
        onFinished()
    }

    private func warmUpDongOverlay() async {
        do {
            let bundle = try await RunvealDongOverlayLoader.load(builtForTrackId: nil)
            RunvealDongOverlayHolder.shared.publish(bundle)
        } catch {
            // 홈에서 다시 시도한다.
            print("[Splash] 동 오버레이 사전 생성 실패 — 홈에서 다시 시도한다: \(error)")
        }
    }

    /// 앱 실행 시 1번 — `docs/api/API.md` §3.1/§9. 받아서 `MetaHolder`에만 둔다.
    private func fetchMeta() async -> Bool {
        do {
            // MetaApi 생성자에 기본 인자가 있어 Swift에서 init()이 막힌다 — iosMain 래퍼를 쓴다.
            try await IosMeta.shared.fetchAndPublish()
            return true
        } catch {
            print("[Splash] GET /api/v1/meta 실패: \(error)")
            return false
        }
    }
}
