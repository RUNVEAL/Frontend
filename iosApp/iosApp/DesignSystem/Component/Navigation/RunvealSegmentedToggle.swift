import SwiftUI

/// 지도 보기 전환 토글(탐험한 길 / 안 가본 길).
struct RunvealSegmentedToggle: View {
    private let options: [String]
    private let selectedIndex: Int
    private let onSelect: (Int) -> Void

    init(options: [String], selectedIndex: Int, onSelect: @escaping (Int) -> Void) {
        self.options = options
        self.selectedIndex = selectedIndex
        self.onSelect = onSelect
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space4) {
            ForEach(options.indices, id: \.self) { index in
                let selected = index == selectedIndex
                Button {
                    onSelect(index)
                } label: {
                    Text(options[index])
                        .runvealTextStyle(
                            selected
                                ? RunvealTheme.typography.bodyMSemibold
                                : RunvealTheme.typography.bodyMRegular
                        )
                        .foregroundStyle(selected ? colors.text.onAccent : colors.text.tertiary)
                        .padding(.horizontal, RunvealSpacing.space16)
                        .frame(maxHeight: .infinity)
                        .background(selected ? colors.accent.default : Color.clear, in: RunvealShapes.full)
                        .contentShape(RunvealShapes.full)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
            }
        }
        .padding(RunvealSpacing.space4)
        .frame(height: RunvealLayout.sizeSegmentedToggle)
        .runvealGlass(RunvealShapes.full)
        .clipShape(RunvealShapes.full)
    }
}

#Preview {
    RunvealSegmentedToggle(options: ["탐험한 길", "안 가본 길"], selectedIndex: 0) { _ in }
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
