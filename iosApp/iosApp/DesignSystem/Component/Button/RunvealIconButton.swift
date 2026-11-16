import SwiftUI

/// L=코스 만들기, 내 위치. M=헤더 옆 작은 버튼(보이는 크기는 작아도 터치 영역은 48 확보).
enum RunvealIconButtonSize { case l, m }

struct RunvealIconButton: View {
    private let icon: RunvealIconSpec
    private let accessibilityLabel: String?
    private let size: RunvealIconButtonSize
    private let onClick: () -> Void

    init(
        icon: RunvealIconSpec,
        accessibilityLabel: String?,
        size: RunvealIconButtonSize = .l,
        onClick: @escaping () -> Void
    ) {
        self.icon = icon
        self.accessibilityLabel = accessibilityLabel
        self.size = size
        self.onClick = onClick
    }

    var body: some View {
        let visibleSize = size == .l ? RunvealLayout.sizeIconButtonL : RunvealLayout.sizeIconButtonM
        return Button(action: onClick) {
            RunvealIcon(icon, tint: RunvealTheme.colors.icon.primary, size: RunvealLayout.sizeIcon)
        }
        .buttonStyle(
            IconButtonStyle(
                visibleSize: visibleSize,
                touchSize: max(visibleSize, RunvealLayout.sizeTouchTargetMin)
            )
        )
        .accessibilityLabel(accessibilityLabel ?? "")
    }
}

/// 글래스 원형 배경 + 눌림 오버레이는 **보이는 크기**에만 그리고, 터치 영역만 48 이상으로 넓힌다
/// (M 사이즈는 보이는 지름 44 < 터치 48).
private struct IconButtonStyle: ButtonStyle {
    let visibleSize: CGFloat
    let touchSize: CGFloat

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .frame(width: visibleSize, height: visibleSize)
            .runvealGlass(RunvealShapes.full)
            .overlay {
                RunvealShapes.full.fill(
                    configuration.isPressed ? RunvealTheme.colors.state.pressedOnSurface : Color.clear
                )
            }
            .frame(width: touchSize, height: touchSize)
            .contentShape(Rectangle())
    }
}

#Preview {
    HStack {
        RunvealIconButton(icon: RunvealIcons.locate, accessibilityLabel: "내 위치") {}
        RunvealIconButton(icon: RunvealIcons.plus, accessibilityLabel: "추가", size: .m) {}
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
