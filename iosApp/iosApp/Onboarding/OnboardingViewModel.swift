import Foundation
import SharedLogic

/// Android `OnboardingViewModel`에 해당한다.
@MainActor
final class OnboardingViewModel: MviViewModel<OnboardingIntent, OnboardingState, OnboardingEffect> {

    init() {
        super.init(initialState: OnboardingState())
    }

    override func handleIntent(_ intent: OnboardingIntent) async {
        switch intent {
        case .finished:
            // "봤다"를 먼저 적고 넘어간다 — 적기 전에 화면을 떠나면 다음 실행에 또 보여주게 된다.
            do {
                try await IosOnboarding.shared.markCompleted()
            } catch {
                // 못 적어도 홈으로는 보낸다 — 다음 실행에 온보딩을 한 번 더 보는 것뿐이다.
                print("[Onboarding] 완료 기록 실패 — 다음 실행에 다시 보여주게 된다: \(error)")
            }
            sendEffect(.navigateToHome)
        }
    }
}
