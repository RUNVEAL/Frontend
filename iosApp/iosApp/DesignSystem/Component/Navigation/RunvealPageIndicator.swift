import SwiftUI

/// 온보딩 단계 표시.
struct RunvealPageIndicator: View {
    private let count: Int
    private let current: Int

    init(count: Int, current: Int) {
        self.count = count
        self.current = current
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space8) {
            ForEach(0..<count, id: \.self) { index in
                let active = index == current
                RunvealShapes.full
                    .fill(active ? colors.icon.primary : colors.border.strong)
                    .frame(width: active ? 20 : 6, height: 6)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: current)
    }
}

#Preview {
    RunvealPageIndicator(count: 3, current: 1)
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
