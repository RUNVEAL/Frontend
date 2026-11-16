import SwiftUI

/// 기록 화면의 기록 목록 한 줄. 왼쪽 타임라인(점 + 그라데이션 선) + 날짜 + 기록 카드(썸네일 + 정보).
/// 리스트에서 쓸 때 아이템 사이 간격은 0으로 두고 이 뷰의 아래 여백(12)만으로 연결선을 잇는다.
struct RunvealRecordListItem<Thumbnail: View>: View {
    private static var timelineWidth: CGFloat { 8 }
    private static var timelineDotSize: CGFloat { 8 }
    private static var timelineLineWidth: CGFloat { 1 }
    private static var timelineLineHeight: CGFloat { 148 }
    private static var thumbnailSize: CGFloat { 80 }

    private let date: String
    private let title: String
    private let distance: String
    private let time: String
    private let pace: String
    private let newRoad: String?
    private let onClick: () -> Void
    @ViewBuilder private let thumbnail: () -> Thumbnail

    init(
        date: String,
        title: String,
        distance: String,
        time: String,
        pace: String,
        newRoad: String? = nil,
        onClick: @escaping () -> Void,
        @ViewBuilder thumbnail: @escaping () -> Thumbnail = { EmptyView() }
    ) {
        self.date = date
        self.title = title
        self.distance = distance
        self.time = time
        self.pace = pace
        self.newRoad = newRoad
        self.onClick = onClick
        self.thumbnail = thumbnail
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(alignment: .top, spacing: RunvealSpacing.space8) {
            timeline

            VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
                Text(date)
                    .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                    .foregroundStyle(colors.text.tertiary)
                card
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity)
        .padding(.bottom, RunvealSpacing.space12)
    }

    private var timeline: some View {
        let colors = RunvealTheme.colors
        let gradient = LinearGradient(
            stops: [
                .init(color: Color(red: 0.9843, green: 0.9373, blue: 0.7373, opacity: 1), location: 0),
                .init(color: Color(red: 0.8745, green: 0.8314, blue: 0.6549, opacity: 0.73), location: 0.59),
                .init(color: Color(red: 0.5843, green: 0.5569, blue: 0.4392, opacity: 0), location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )

        return ZStack(alignment: .topLeading) {
            Rectangle()
                .fill(gradient)
                .frame(width: Self.timelineLineWidth, height: Self.timelineLineHeight)
                .padding(.top, RunvealSpacing.space4)
            RunvealShapes.full
                .fill(colors.accent.default)
                .overlay { RunvealShapes.full.stroke(colors.progress.filled, lineWidth: 2) }
                .frame(width: Self.timelineDotSize, height: Self.timelineDotSize)
        }
        .frame(width: Self.timelineWidth, height: Self.timelineLineHeight + RunvealSpacing.space4, alignment: .topLeading)
    }

    private var card: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            HStack(spacing: RunvealSpacing.space12) {
                ZStack { thumbnail() }
                    .frame(width: Self.thumbnailSize, height: Self.thumbnailSize)
                    .background(colors.bg.map, in: RunvealShapes.radius8)
                    .overlay { RunvealShapes.radius8.stroke(colors.border.subtle, lineWidth: 1) }
                    .clipShape(RunvealShapes.radius8)

                VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                    Text(title)
                        .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                        .foregroundStyle(colors.text.primary)
                    HStack(spacing: RunvealSpacing.space8) {
                        Text(distance)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                        Text(time)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                        Text(pace)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                    }
                    if let newRoad {
                        Text(newRoad)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.accent)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(RunvealSpacing.space12)
            .frame(maxWidth: .infinity)
            .frame(height: 106)
            .background(colors.surface.default, in: RunvealShapes.radius16)
            .overlay { RunvealShapes.radius16.stroke(colors.border.default, lineWidth: 1) }
        }
        .runvealPressOverlayStyle(shape: RunvealShapes.radius16, overlay: colors.state.pressedOnSurface)
    }
}

#Preview {
    VStack(spacing: 0) {
        RunvealRecordListItem(
            date: "11월 4일 수요일",
            title: "성수동 한 바퀴",
            distance: "5.21km",
            time: "31:40",
            pace: "6′05″",
            newRoad: "새 길 +1.30km",
            onClick: {}
        )
        RunvealRecordListItem(
            date: "11월 2일 월요일",
            title: "저녁 러닝",
            distance: "3.02km",
            time: "18:12",
            pace: "6′02″",
            onClick: {}
        )
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
