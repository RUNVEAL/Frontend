import SwiftUI

/// 지도 범례. 배경 없이 지도 위에 바로 올린다.
enum RunvealLegendItem: CaseIterable, Hashable {
    case new, today, explored

    var label: String {
        switch self {
        case .new: return RunvealDesignSystemStrings.legendNew
        case .today: return RunvealDesignSystemStrings.legendToday
        case .explored: return RunvealDesignSystemStrings.legendExplored
        }
    }

    func color(_ colors: RunvealColors) -> Color {
        switch self {
        case .new: return colors.map.roadNew
        case .today: return colors.map.roadToday
        case .explored: return colors.map.roadExplored
        }
    }
}

struct RunvealLegend: View {
    private let items: [RunvealLegendItem]

    init(items: [RunvealLegendItem] = [.new, .today, .explored]) {
        self.items = items
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space12) {
            ForEach(items, id: \.self) { item in
                HStack(spacing: 6) {
                    swatch(color: item.color(colors), glowing: item == .new)
                    Text(item.label)
                        .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
            }
        }
    }

    @ViewBuilder
    private func swatch(color: Color, glowing: Bool) -> some View {
        let base = RunvealShapes.full.fill(color).frame(width: 14, height: 3)
        if glowing {
            base.runvealDropShadow(RunvealEffects.glowGold)
        } else {
            base
        }
    }
}

#Preview {
    RunvealLegend()
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
