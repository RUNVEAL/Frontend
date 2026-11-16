import SwiftUI

/// OnCard=카드 위(불투명), OnMap=지도 위(글래스).
enum RunvealChipDropdownUsage { case onCard, onMap }

/// 누르면 선택지가 열리는 칩. 라벨 + icon/expand 16.
struct RunvealChipDropdown: View {
    private let label: String
    private let usage: RunvealChipDropdownUsage
    private let onClick: () -> Void

    init(label: String, usage: RunvealChipDropdownUsage = .onCard, onClick: @escaping () -> Void) {
        self.label = label
        self.usage = usage
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            let row = HStack(spacing: RunvealSpacing.space4) {
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                    .foregroundStyle(colors.text.primary)
                RunvealIcon(RunvealIcons.expand, tint: colors.icon.primary, size: 16)
            }
            .padding(.leading, RunvealSpacing.space16)
            .padding(.trailing, RunvealSpacing.space12)
            .frame(height: RunvealLayout.sizeChipHeight)

            Group {
                switch usage {
                case .onCard:
                    row
                        .background(colors.surface.raised, in: RunvealShapes.full)
                        .overlay { RunvealShapes.full.stroke(colors.border.default, lineWidth: 1) }
                case .onMap:
                    row.runvealGlass(RunvealShapes.full)
                }
            }
            .frame(minHeight: RunvealLayout.sizeTouchTargetMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    RunvealChipDropdown(label: "거리순") {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
