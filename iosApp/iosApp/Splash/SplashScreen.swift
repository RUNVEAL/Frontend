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

    /// 사전 생성이 끝났는지 되묻는 간격 — 상한만 지키면 되니 촘촘할 필요가 없다.
    private static let overlayWarmUpPollInterval: Duration = .milliseconds(100)

    /// 동 오버레이 사전 생성이 끝났는가(성공·실패 둘 다). 상한까지만 기다리는 데 쓴다 —
    /// `waitForOverlayWarmUp()` 주석 참고.
    @State private var overlayWarmUpFinished = false

    var body: some View {
        ZStack(alignment: .bottom) {
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

            // 임시 진단 패널 — 테스터 화면 녹화로 멈춘 지점을 읽는다. 홈으로 넘어가면 스플래시와
            // 함께 사라지므로, 이 패널이 "홈으로 전환 호출"을 띄운 채 남아 있으면 전환은 됐는데
            // 홈이 안 그려진 것이다. 원인을 찾으면 지운다(RunvealTrace.swift).
            RunvealTraceOverlay()
        }
        .task { await bootstrap() }
    }

    /// **메인 액터에서 돈다.** `onFinished()`가 `RunvealAppNavigator`(`@MainActor`)의 `@Published`
    /// route를 바꾸는데, 비격리 `async` 함수는 호출자의 액터를 물려받지 않고 글로벌 풀로 넘어가서
    /// (SE-0338) 그 변경이 메인 아닌 스레드에서 일어난다 — SwiftUI가 화면을 갈아 끼우지 않아
    /// 스플래시가 그대로 남는다. `@State` 플래그를 읽고 쓰는 쪽도 같은 이유로 여기 묶는다.
    @MainActor
    private func bootstrap() async {
        RunvealTrace.step(.bootstrapStart)
        // 홈이 홀더로 받아 쓰니 스플래시를 넘어가도 계속 돌아야 한다 — 그래서 구조적 자식이 아니라
        // 별도 Task로 띄우고, 상한은 "기다리는 쪽"에만 둔다.
        Task {
            await warmUpDongOverlay()
            overlayWarmUpFinished = true
            RunvealTrace.step(.overlayFlagSet)
        }
        let metaSucceeded = await fetchMeta()
        RunvealTrace.step(metaSucceeded ? .metaDone : .metaFailed)

        if !metaSucceeded {
            try? await Task.sleep(for: Self.minimumVisibleDuration)
        }
        // 오버레이가 준비될 때까지(상한까지) 기다렸다 홈으로 넘어간다.
        await waitForOverlayWarmUp()

        // TODO: 로그인 여부 확인 후 로그인 화면/홈으로 분기. 로그인 기능 구현 전까지는 항상 홈으로 이동
        //  (Android도 같은 TODO 상태다).
        RunvealTrace.step(.navigatingHome)
        onFinished()
    }

    /// 오버레이 사전 생성을 [overlayWarmUpTimeout]까지만 기다린다.
    ///
    /// `Task.value`를 기다리면 안 된다 — 취소에 반응하지 않아서, 그걸 자식 태스크로 감싸
    /// `Task.sleep`과 `withTaskGroup` 레이스를 걸면 상한 쪽이 먼저 끝나도 그룹이 **남은 자식을
    /// 계속 기다려** 리턴하지 않는다. 그래서 상한이 무의미해지고, 전국 경계 파싱이 끝날 때까지
    /// 스플래시에서 홈으로 넘어가지 못했다. 완료 플래그를 짧게 되묻는 쪽이 상한을 실제로 지킨다.
    @MainActor
    private func waitForOverlayWarmUp() async {
        let deadline = ContinuousClock.now.advanced(by: Self.overlayWarmUpTimeout)
        while !overlayWarmUpFinished && ContinuousClock.now < deadline {
            do {
                try await Task.sleep(for: Self.overlayWarmUpPollInterval)
            } catch {
                // 화면이 사라져 `.task`가 취소된 것 — 더 기다릴 이유가 없다.
                return
            }
        }
        RunvealTrace.step(overlayWarmUpFinished ? .overlayWaitFinished : .overlayWaitTimedOut)
    }

    /// **메인 액터 밖에서 돈다**(`nonisolated`). 전국 경계 파싱과 Kotlin Map → Swift Dictionary
    /// 변환(동 3천여 개의 폴리곤)이 무거워서, 메인에서 돌리면 그 동안 화면이 멈추는 건 물론
    /// 상한을 재는 `waitForOverlayWarmUp()`까지 같이 멈춰 상한이 무의미해진다.
    private nonisolated func warmUpDongOverlay() async {
        RunvealTrace.step(.overlayWarmUpStart)
        do {
            let bundle = try await RunvealDongOverlayLoader.load(builtForTrackId: nil)
            RunvealTrace.step(.overlayBuilt)
            // 홀더는 `@MainActor`다 — 비격리 컨텍스트에서 부르니 await로 넘긴다.
            await RunvealDongOverlayHolder.shared.publish(bundle)
            RunvealTrace.step(.overlayPublished)
        } catch {
            // 홈에서 다시 시도한다.
            RunvealTrace.step(.overlayFailed, "\(error)")
        }
    }

    /// 앱 실행 시 1번 — `docs/api/API.md` §3.1/§9. 받아서 `MetaHolder`에만 둔다.
    private nonisolated func fetchMeta() async -> Bool {
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
