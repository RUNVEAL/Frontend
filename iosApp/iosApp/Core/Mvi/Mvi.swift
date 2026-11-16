import Foundation

/// MVI 화면 상태 — ViewModel이 한 번에 들고 있는 값 전부.
protocol UiState {}

/// MVI 사용자 의도 — 화면에서 ViewModel로 보내는 액션.
protocol UiIntent {}

/// MVI 이펙트 — 한 번만 소비되는 부수효과(네비게이션, 토스트 등).
/// 효과가 없는 화면은 [NoEffect]를 쓴다(Android는 `Nothing`).
protocol UiEffect {}

/// 부수효과가 없는 화면의 Effect 타입 — Android의 `Nothing` 자리.
/// Swift에는 값이 없는 타입을 제네릭 인자로 쓸 방법이 없어서 케이스 없는 enum으로 대신한다.
enum NoEffect: UiEffect {}
