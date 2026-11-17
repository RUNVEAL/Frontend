import SwiftUI

/// 러닝 시작 원형 버튼.
struct RunvealRunStartButton: View {
    private let label: String
    private let onClick: () -> Void

    init(label: String = RunvealDesignSystemStrings.runStartButtonDefaultLabel, onClick: @escaping () -> Void) {
        self.label = label
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            VStack(spacing: RunvealSpacing.space4) {
                RunvealIcon(RunvealIcons.play, tint: colors.icon.onAccent)
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.labelButton)
                    .foregroundStyle(colors.text.onAccent)
            }
            .frame(width: RunvealLayout.sizeRunStart, height: RunvealLayout.sizeRunStart)
            .background(colors.accent.default, in: RunvealShapes.full)
        }
        .runvealPressOverlayStyle(shape: RunvealShapes.full, overlay: colors.state.pressedOnAccent)
        .accessibilityLabel(label)
    }
}

#Preview {
    RunvealRunStartButton {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
