import SwiftUI

enum RunvealRouteStepType { case start, waypoint, newRoad, end }

/// 코스 상세 경로 정보 한 줄. 줄 사이 연결선은 [showConnector], 마지막 줄은 꺼서 끊는다.
struct RunvealRouteStep: View {
    private static let dotSize: CGFloat = 8
    private static let connectorWidth: CGFloat = 1
    private static let connectorHeight: CGFloat = 8

    private let place: String
    private let distance: String
    private let type: RunvealRouteStepType
    private let note: String?
    private let showConnector: Bool

    init(
        place: String,
        distance: String,
        type: RunvealRouteStepType,
        note: String? = nil,
        showConnector: Bool = true
    ) {
        self.place = place
        self.distance = distance
        self.type = type
        self.note = note
        self.showConnector = showConnector
    }

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: RunvealSpacing.space8) {
                dot
                HStack {
                    // place + note는 한 줄에 나란히 — note는 줄 아래가 아니라 place 옆에 온다.
                    HStack(alignment: .top, spacing: RunvealSpacing.space4) {
                        Text(place)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                        if let note {
                            Text(note)
                                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                                .foregroundStyle(colors.text.tertiary)
                        }
                    }
                    Spacer()
                    Text(distance)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
                .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: .infinity)

            if showConnector {
                Rectangle()
                    .fill(colors.border.default)
                    .frame(width: Self.connectorWidth, height: Self.connectorHeight)
                    .padding(.leading, (Self.dotSize - Self.connectorWidth) / 2)
            }
        }
    }

    @ViewBuilder
    private var dot: some View {
        let colors = RunvealTheme.colors
        switch type {
        case .start:
            RunvealShapes.full
                .fill(colors.accent.glow)
                .overlay { RunvealShapes.full.stroke(colors.icon.tertiary, lineWidth: 1) }
                .frame(width: Self.dotSize, height: Self.dotSize)
        case .waypoint:
            RunvealShapes.full
                .stroke(colors.icon.tertiary, lineWidth: 1)
                .frame(width: Self.dotSize, height: Self.dotSize)
        case .newRoad:
            RunvealShapes.full
                .fill(colors.accent.default)
                .frame(width: Self.dotSize, height: Self.dotSize)
                .runvealDropShadow(RunvealEffects.glowGold)
        case .end:
            RunvealShapes.full
                .fill(colors.icon.primary)
                .overlay { RunvealShapes.full.stroke(colors.icon.tertiary, lineWidth: 1) }
                .frame(width: Self.dotSize, height: Self.dotSize)
        }
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 0) {
        RunvealRouteStep(place: "성수역 2번 출구", distance: "0km", type: .start)
        RunvealRouteStep(place: "서울숲", distance: "1.2km", type: .waypoint)
        RunvealRouteStep(place: "뚝섬한강공원", distance: "3.5km", type: .newRoad, note: "새로 탐험한 길")
        RunvealRouteStep(place: "성수역 2번 출구", distance: "5.21km", type: .end, showConnector: false)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
