import SwiftUI

enum RunvealCourseListItemRunState { case run, notRun }

/// 내 코스 목록 한 줄. 아직 안 달린 코스는 상태 글자를 text/accent로 강조.
struct RunvealCourseListItem: View {
    private let title: String
    private let distance: String
    private let time: String
    private let runState: RunvealCourseListItemRunState
    private let thumbnailType: RunvealCourseThumbnailType
    private let tag: String?
    private let runCount: String?
    private let lastRun: String?
    private let showDivider: Bool
    private let onClick: () -> Void
    private let onMoreClick: () -> Void
    private let onPlayClick: () -> Void

    init(
        title: String,
        distance: String,
        time: String,
        runState: RunvealCourseListItemRunState,
        thumbnailType: RunvealCourseThumbnailType = .oneWay,
        tag: String? = nil,
        runCount: String? = nil,
        lastRun: String? = nil,
        showDivider: Bool = false,
        onClick: @escaping () -> Void,
        onMoreClick: @escaping () -> Void,
        onPlayClick: @escaping () -> Void
    ) {
        self.title = title
        self.distance = distance
        self.time = time
        self.runState = runState
        self.thumbnailType = thumbnailType
        self.tag = tag
        self.runCount = runCount
        self.lastRun = lastRun
        self.showDivider = showDivider
        self.onClick = onClick
        self.onMoreClick = onMoreClick
        self.onPlayClick = onPlayClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: 0) {
            HStack(spacing: RunvealSpacing.space16) {
                RunvealCourseThumbnail(type: thumbnailType)

                VStack(alignment: .leading, spacing: RunvealSpacing.space2) {
                    Text(title)
                        .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                        .foregroundStyle(colors.text.primary)
                    HStack(spacing: RunvealSpacing.space8) {
                        Text(distance)
                            .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                            .foregroundStyle(colors.text.primary)
                        Text(time)
                            .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                            .foregroundStyle(colors.text.primary)
                        if let tag { RunvealTag(label: tag) }
                    }
                    if runState == .run {
                        Text([runCount, lastRun].compactMap { $0 }.joined(separator: " · "))
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.tertiary)
                    } else {
                        Text(RunvealDesignSystemStrings.courseListItemNotRun)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.accent)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                // 줄 전체 터치는 썸네일·정보 영역만 받는다 — 더보기/재생 버튼이 가로채지 않게.
                .contentShape(Rectangle())
                .onTapGesture(perform: onClick)

                Button(action: onMoreClick) {
                    RunvealIcon(RunvealIcons.more, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
                        .frame(width: RunvealLayout.sizeTouchTargetMin, height: RunvealLayout.sizeTouchTargetMin)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                Button(action: onPlayClick) {
                    RunvealIcon(RunvealIcons.play, tint: colors.icon.primary, size: 16)
                        .frame(width: 44, height: 44)
                        .background(colors.surface.default, in: RunvealShapes.full)
                        .overlay { RunvealShapes.full.stroke(colors.accent.default, lineWidth: 1) }
                        .contentShape(RunvealShapes.full)
                }
                .buttonStyle(.plain)
            }
            .padding(.vertical, RunvealSpacing.space12)
            .frame(maxWidth: .infinity)

            if showDivider {
                Rectangle()
                    .fill(colors.border.subtle)
                    .frame(height: 1)
            }
        }
    }
}

#Preview {
    RunvealCourseListItem(
        title: "성수동 한 바퀴",
        distance: "5.21km",
        time: "32:10",
        runState: .run,
        thumbnailType: .circulation,
        tag: "순환",
        runCount: "3회 완주",
        lastRun: "2일 전",
        onClick: {},
        onMoreClick: {},
        onPlayClick: {}
    )
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
