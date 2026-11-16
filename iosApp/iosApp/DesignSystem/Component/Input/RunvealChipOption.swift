import SwiftUI

/// 하나를 고르는 선택 칩(거리 5km 등). 보이는 높이는 32지만 터치 영역은 48 이상.
struct RunvealChipOption: View {
    private let label: String
    private let selected: Bool
    private let onClick: () -> Void

    init(label: String, selected: Bool, onClick: @escaping () -> Void) {
        self.label = label
        self.selected = selected
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(selected ? colors.text.onInverse : colors.text.primary)
                .padding(.horizontal, RunvealSpacing.space12)
                .frame(height: RunvealLayout.sizeChipHeight)
                .background {
                    if selected {
                        RunvealShapes.full.fill(colors.surface.inverse)
                    } else {
                        RunvealShapes.full.stroke(colors.border.strong, lineWidth: 1)
                    }
                }
                .frame(minHeight: RunvealLayout.sizeTouchTargetMin)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview {
    HStack(spacing: 8) {
        RunvealChipOption(label: "5km", selected: true) {}
        RunvealChipOption(label: "10km", selected: false) {}
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
