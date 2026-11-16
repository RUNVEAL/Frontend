import Foundation

/// MVI 뼈대. 화면은 [onIntent]로 의도를 보내고, 그 처리는 [handleIntent]에서 [setState]로 상태를
/// 갱신하거나 [sendEffect]로 한 번만 소비되는 부수효과를 내보낸다. 효과가 필요 없는 화면은
/// Effect 타입 인자로 [NoEffect]를 쓴다.
///
/// Android `MviViewModel`과 같은 구조다 — `StateFlow` 자리에 `@Published`,
/// `Channel`+`receiveAsFlow` 자리에 버퍼링하는 `AsyncStream`을 쓴다(둘 다 소비자가 하나다).
/// `viewModelScope` 자리는 `Task`이고, 뷰모델이 사라질 때 [cancelRunningTasks]로 같이 끊는다.
@MainActor
class MviViewModel<Intent: UiIntent, State: UiState, Effect: UiEffect>: ObservableObject {
    @Published private(set) var state: State

    /// 화면이 `.collectEffect(viewModel.effects) { ... }`로 한 번만 구독한다.
    let effects: AsyncStream<Effect>

    private let effectContinuation: AsyncStream<Effect>.Continuation
    private var runningTasks: [Task<Void, Never>] = []

    init(initialState: State) {
        self.state = initialState
        var capturedContinuation: AsyncStream<Effect>.Continuation!
        self.effects = AsyncStream(bufferingPolicy: .unbounded) { continuation in
            capturedContinuation = continuation
        }
        self.effectContinuation = capturedContinuation
    }

    deinit {
        effectContinuation.finish()
    }

    func onIntent(_ intent: Intent) {
        launch { [weak self] in
            await self?.handleIntent(intent)
        }
    }

    /// 하위 클래스가 반드시 구현한다. Android의 `protected abstract suspend fun handleIntent`.
    func handleIntent(_ intent: Intent) async {
        assertionFailure("handleIntent(_:)를 하위 클래스에서 구현해야 한다")
    }

    func setState(_ reduce: (State) -> State) {
        state = reduce(state)
    }

    func sendEffect(_ effect: Effect) {
        effectContinuation.yield(effect)
    }

    /// `viewModelScope.launch` 대응 — 화면이 사라질 때 [cancelRunningTasks]로 함께 취소된다.
    func launch(_ operation: @escaping @MainActor () async -> Void) {
        let task = Task { @MainActor in
            await operation()
        }
        runningTasks.append(task)
        runningTasks.removeAll { $0.isCancelled }
    }

    /// Android는 ViewModel이 clear될 때 `viewModelScope`가 자동으로 끊기지만, Swift의 `Task`는
    /// 참조가 사라져도 계속 돈다 — 화면이 사라질 때(`onDisappear`) 직접 불러 준다.
    func cancelRunningTasks() {
        runningTasks.forEach { $0.cancel() }
        runningTasks.removeAll()
    }
}
