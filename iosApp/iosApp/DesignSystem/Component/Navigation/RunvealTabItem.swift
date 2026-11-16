import SwiftUI

/// GNB 탭 하나. [RunvealTabBar]가 4개를 HStack으로 묶어서 쓴다.
struct RunvealTabItem: View {
    private let icon: RunvealIconSpec
    private let label: String
    private let active: Bool
    private let onClick: () -> Void

    init(icon: RunvealIconSpec, label: String, active: Bool, onClick: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.active = active
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            VStack(spacing: RunvealSpacing.space2) {
                RunvealIcon(icon, tint: active ? colors.icon.onAccent : colors.icon.tertiary)
                Text(label)
                    .runvealTextStyle(
                        active
                            ? RunvealTheme.typography.captionSSemibold
                            : RunvealTheme.typography.captionSRegular
                    )
                    .foregroundStyle(active ? colors.text.onAccent : colors.text.tertiary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(active ? colors.accent.default : Color.clear, in: RunvealShapes.full)
            .contentShape(RunvealShapes.full)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(active ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview {
    RunvealTabItem(icon: RunvealIcons.home, label: "홈", active: true) {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
