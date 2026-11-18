import SharedLogic
import SwiftUI

/// 고도 프로필 그래프. 디자인시스템엔 이 차트 전용 컴포넌트가 없어 `RouteDto.elevationProfile`
/// 좌표로 여기서 직접 그린다 — Y축 레이블(최고/중간/최저)은 프로필 값에서 그대로 계산한다.
struct ElevationChart: View {
    let points: [ElevationPoint]

    private static let chartHeight: CGFloat = 80
    private static let legendWidth: CGFloat = 30

    var body: some View {
        if points.count < 2 {
            EmptyView()
        } else {
            chart
        }
    }

    private var chart: some View {
        let colors = RunvealTheme.colors
        let minElevation = points.map(\.elevationM).min() ?? 0
        let maxElevation = points.map(\.elevationM).max() ?? 0
        let midElevation = (minElevation + maxElevation) / 2

        return HStack(spacing: RunvealSpacing.space8) {
            VStack(alignment: .leading) {
                legendText("\(Int(maxElevation.rounded()))m")
                Spacer()
                legendText("\(Int(midElevation.rounded()))m")
                Spacer()
                legendText("\(Int(minElevation.rounded()))m")
            }
            .frame(width: Self.legendWidth)

            GeometryReader { geometry in
                let size = geometry.size
                let maxDistance = max(points.last?.cumulativeDistanceM ?? 1, 1)
                let elevationRange = max(maxElevation - minElevation, 1)

                // ViewBuilder 클로저 안에서는 `func` 선언을 할 수 없다(result builder 제약) —
                // 같은 일을 하는 클로저 상수로 둔다.
                let point: (ElevationPoint) -> CGPoint = { item in
                    CGPoint(
                        x: CGFloat(item.cumulativeDistanceM / maxDistance) * size.width,
                        y: size.height - CGFloat((item.elevationM - minElevation) / elevationRange) * size.height
                    )
                }

                let linePath = Path { path in
                    for (index, item) in points.enumerated() {
                        let position = point(item)
                        if index == 0 {
                            path.move(to: position)
                        } else {
                            path.addLine(to: position)
                        }
                    }
                }
                let fillPath = Path { path in
                    path.addPath(linePath)
                    path.addLine(to: CGPoint(x: size.width, y: size.height))
                    path.addLine(to: CGPoint(x: 0, y: size.height))
                    path.closeSubpath()
                }

                ZStack {
                    fillPath.fill(
                        LinearGradient(
                            colors: [colors.accent.default.opacity(0.3), colors.accent.default.opacity(0)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    linePath.stroke(colors.accent.default, lineWidth: 2)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.chartHeight)
    }

    private func legendText(_ text: String) -> some View {
        Text(text)
            .runvealTextStyle(RunvealTheme.typography.unitS)
            .foregroundStyle(RunvealTheme.colors.text.tertiary)
    }
}
