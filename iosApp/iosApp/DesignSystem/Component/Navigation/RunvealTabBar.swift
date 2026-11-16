import SwiftUI

struct RunvealTabItemSpec: Identifiable {
    let id = UUID()
    let icon: RunvealIconSpec
    let label: String

    init(icon: RunvealIconSpec, label: String) {
        self.icon = icon
        self.label = label
    }
}

/// Floating=단독(글래스), Embedded=Home Bottom Panel 안(글래스 효과는 바깥 패널이 담당).
enum RunvealTabBarContainer { case floating, embedded }

/// 플로팅 GNB.
struct RunvealTabBar: View {
    private let items: [RunvealTabItemSpec]
    private let selectedIndex: Int
    private let container: RunvealTabBarContainer
    private let onSelect: (Int) -> Void

    init(
        items: [RunvealTabItemSpec],
        selectedIndex: Int,
        container: RunvealTabBarContainer = .floating,
        onSelect: @escaping (Int) -> Void
    ) {
        self.items = items
        self.selectedIndex = selectedIndex
        self.container = container
        self.onSelect = onSelect
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let row = HStack(spacing: RunvealSpacing.space4) {
            ForEach(items.indices, id: \.self) { index in
                RunvealTabItem(
                    icon: items[index].icon,
                    label: items[index].label,
                    active: index == selectedIndex
                ) {
                    onSelect(index)
                }
            }
        }
        .padding(RunvealSpacing.space4)
        .frame(maxWidth: .infinity)
        .frame(height: RunvealLayout.sizeTabBar)

        switch container {
        case .floating:
            row.runvealGlass(RunvealShapes.full).clipShape(RunvealShapes.full)
        case .embedded:
            row
                .background(colors.surface.default, in: RunvealShapes.full)
                .overlay { RunvealShapes.full.stroke(colors.border.default, lineWidth: 1) }
                .clipShape(RunvealShapes.full)
        }
    }
}

#Preview {
    RunvealTabBar(
        items: [
            RunvealTabItemSpec(icon: RunvealIcons.home, label: "홈"),
            RunvealTabItemSpec(icon: RunvealIcons.course, label: "내 코스"),
            RunvealTabItemSpec(icon: RunvealIcons.record, label: "기록"),
            RunvealTabItemSpec(icon: RunvealIcons.my, label: "MY"),
        ],
        selectedIndex: 0
    ) { _ in }
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
