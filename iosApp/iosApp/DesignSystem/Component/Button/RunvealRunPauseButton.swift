import SwiftUI

/// 러닝 중 일시정지 원형 버튼. [RunvealRunStartButton]보다 작다(80).
struct RunvealRunPauseButton: View {
    private static let diameter: CGFloat = 80

    private let label: String
    private let onClick: () -> Void

    init(label: String = RunvealDesignSystemStrings.runPauseButtonDefaultLabel, onClick: @escaping () -> Void) {
        self.label = label
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            VStack(spacing: RunvealSpacing.space4) {
                RunvealIcon(RunvealIcons.pause, tint: colors.icon.onAccent)
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                    .foregroundStyle(colors.text.onAccent)
            }
            .frame(width: Self.diameter, height: Self.diameter)
            .background(colors.accent.default, in: RunvealShapes.full)
        }
        .runvealPressOverlayStyle(shape: RunvealShapes.full, overlay: colors.state.pressedOnAccent)
        .accessibilityLabel(label)
    }
}

#Preview {
    RunvealRunPauseButton {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
