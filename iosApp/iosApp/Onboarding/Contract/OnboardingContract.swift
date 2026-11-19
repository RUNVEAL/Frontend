import Foundation

enum OnboardingIntent: UiIntent {
    /// 마지막 장에서 "시작하기"를 눌렀거나, 아무 장에서 "건너뛰기"를 눌렀다 — 둘 다 온보딩을 끝낸다.
    case finished
}

/// 온보딩은 보여주기만 하는 화면이라 ViewModel이 들고 있을 상태가 없다 — 지금 몇 번째 장인지는
/// 스와이프·버튼과 함께 움직이는 값이라 화면이 `@State`로 들고 있다(Android도 같다).
struct OnboardingState: UiState {}

enum OnboardingEffect: UiEffect {
    case navigateToHome
}
