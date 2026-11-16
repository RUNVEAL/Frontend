import SwiftUI

/// Button/IconButton/RunStartButton처럼 누르면 위에 덮는 오버레이 색을 그린다
/// (design/runveal-components.json의 `states.pressed.overlay`). 색 자체를 바꾸지 않고
/// 위에 반투명 색을 한 겹 더 얹는 방식 — conventions.pressed 그대로.
///
/// Android는 `Modifier.runvealPressOverlay(shape, overlay, pressed)`로 같은 일을 한다. SwiftUI에서는
/// 눌림 상태를 ButtonStyle만 알 수 있어서 Modifier가 아니라 ButtonStyle로 구현한다.
struct RunvealPressOverlayStyle<S: Shape>: ButtonStyle {
    let shape: S
    let overlay: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .overlay { shape.fill(configuration.isPressed ? overlay : Color.clear) }
            .contentShape(shape)
    }
}

extension View {
    /// 눌림 오버레이를 쓰는 버튼에 붙인다. `Button { } label: { }`의 스타일로 적용한다.
    func runvealPressOverlayStyle<S: Shape>(shape: S, overlay: Color) -> some View {
        buttonStyle(RunvealPressOverlayStyle(shape: shape, overlay: overlay))
    }
}
