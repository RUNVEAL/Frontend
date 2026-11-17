import SwiftUI

extension View {
    /// 화면이 보이는 동안 [effects]를 구독해 한 번만 소비되는 부수효과를 처리한다.
    /// Android `CollectEffect(effect, onEffect)`와 같은 역할.
    func collectEffect<Effect: UiEffect>(
        _ effects: AsyncStream<Effect>,
        perform onEffect: @escaping (Effect) async -> Void
    ) -> some View {
        task {
            for await effect in effects {
                await onEffect(effect)
            }
        }
    }
}
