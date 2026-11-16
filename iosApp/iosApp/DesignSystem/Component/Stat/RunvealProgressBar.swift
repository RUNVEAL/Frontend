import SwiftUI

/// 탐험률 진행 바. [filledPercent]=기존 탐험률, [gainedPercent]=이번 러닝으로 늘어난 만큼
/// (0이면 숨김). gained는 filled 끝에서 4pt 겹쳐 시작하고, 보이는 길이는 최소 8pt
/// (2%처럼 작아도 보이게).
struct RunvealProgressBar: View {
    private let filledPercent: Double
    private let gainedPercent: Double

    init(filledPercent: Double, gainedPercent: Double) {
        self.filledPercent = filledPercent
        self.gainedPercent = gainedPercent
    }

    var body: some View {
        let colors = RunvealTheme.colors
        GeometryReader { geometry in
            let totalWidth = geometry.size.width
            let filledWidth = totalWidth * (min(max(filledPercent, 0), 100) / 100)

            ZStack(alignment: .leading) {
                RunvealShapes.full.fill(colors.progress.filled)
                    .frame(width: filledWidth)

                if gainedPercent > 0 {
                    let gainedWidth = max(totalWidth * (min(max(gainedPercent, 0), 100) / 100), 8)
                    let startX = max(filledWidth - 4, 0)
                    RunvealShapes.full.fill(colors.progress.gained)
                        .runvealDropShadow(RunvealEffects.glowGold)
                        .frame(width: gainedWidth)
                        .offset(x: startX)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(height: RunvealLayout.sizeProgressBar)
        .background(colors.progress.track, in: RunvealShapes.full)
        .clipShape(RunvealShapes.full)
    }
}

#Preview {
    RunvealProgressBar(filledPercent: 35, gainedPercent: 2)
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
