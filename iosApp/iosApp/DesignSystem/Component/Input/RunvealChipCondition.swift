import SwiftUI

enum RunvealChipConditionIcon { case check, filter }
enum RunvealChipConditionUsage { case onCard, onMap }

/// 켜고 끄는 조건 칩(안 가본 길 우선, 러닝 필터). [label]이 없으면 아이콘만 보인다.
struct RunvealChipCondition: View {
    private let checked: Bool
    private let label: String?
    private let icon: RunvealChipConditionIcon
    private let usage: RunvealChipConditionUsage
    private let onCheckedChange: (Bool) -> Void

    init(
        checked: Bool,
        label: String? = nil,
        icon: RunvealChipConditionIcon = .check,
        usage: RunvealChipConditionUsage = .onCard,
        onCheckedChange: @escaping (Bool) -> Void
    ) {
        self.checked = checked
        self.label = label
        self.icon = icon
        self.usage = usage
        self.onCheckedChange = onCheckedChange
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let iconSpec = icon == .check ? RunvealIcons.check : RunvealIcons.filter
        let contentColor = checked ? colors.accent.glow : colors.icon.tertiary
        let labelColor = checked ? colors.text.accent : colors.text.tertiary
        let borderColor = checked ? colors.accent.glow : colors.border.default

        return Button {
            onCheckedChange(!checked)
        } label: {
            let row = HStack(spacing: RunvealSpacing.space4) {
                RunvealIcon(iconSpec, tint: contentColor, size: 16)
                if let label {
                    Text(label)
                        .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                        .foregroundStyle(labelColor)
                        .padding(.trailing, RunvealSpacing.space4)
                }
            }
            .padding(.horizontal, RunvealSpacing.space12)
            .frame(height: RunvealLayout.sizeChipHeight)

            Group {
                switch usage {
                case .onCard:
                    row.background(colors.surface.raised, in: RunvealShapes.full)
                case .onMap:
                    row.runvealGlass(RunvealShapes.full)
                }
            }
            .overlay { RunvealShapes.full.stroke(borderColor, lineWidth: 1) }
            .frame(minHeight: RunvealLayout.sizeTouchTargetMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(checked ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview {
    HStack(spacing: 8) {
        RunvealChipCondition(checked: true, label: "안 가본 길 우선") { _ in }
        RunvealChipCondition(checked: false, icon: .filter) { _ in }
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
