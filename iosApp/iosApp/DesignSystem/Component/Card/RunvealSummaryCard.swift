import SwiftUI

/// 홈 요약 카드. Home Bottom Panel 안에서만 쓴다(배경 없음) — 카드 전체 터치 시 동 상세.
struct RunvealSummaryCard<Thumbnail: View>: View {
    private let dongName: String
    private let ratePercent: Int
    private let tagLabel: String?
    private let onClick: () -> Void
    private let onChevronClick: (() -> Void)?
    @ViewBuilder private let thumbnail: () -> Thumbnail

    /// - Parameters:
    ///   - tagLabel: 동 이름 뒤 꼬리표("내 동네" / "현위치"). nil이면 안 붙는다. 어떤 동일 때 어떤
    ///     문구인지는 **호출부가 정한다** — 디자인시스템은 꼬리표 한 줄만 알면 된다.
    ///   - onChevronClick: 오른쪽 `>`를 눌렀을 때. nil이면 아이콘을 그리지 않는다 — 눌러도 아무 일
    ///     없는 아이콘을 남기면 고장으로 보인다.
    init(
        dongName: String,
        ratePercent: Int,
        tagLabel: String? = nil,
        onClick: @escaping () -> Void,
        onChevronClick: (() -> Void)? = nil,
        @ViewBuilder thumbnail: @escaping () -> Thumbnail = { EmptyView() }
    ) {
        self.dongName = dongName
        self.ratePercent = ratePercent
        self.tagLabel = tagLabel
        self.onClick = onClick
        self.onChevronClick = onChevronClick
        self.thumbnail = thumbnail
    }

    var body: some View {
        let colors = RunvealTheme.colors
        // `>`는 카드 Button **밖**에 형제로 둔다 — Button 안에 Button을 넣으면 어느 쪽이 탭을 받는지
        // 플랫폼 동작에 맡겨야 한다. 카드 터치(동 상세)와 아이콘 터치(줌인)는 다른 동작이라
        // 확실히 갈라야 한다.
        return HStack(spacing: RunvealSpacing.space8) {
            Button(action: onClick) {
                HStack(spacing: RunvealSpacing.space8) {
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(spacing: RunvealSpacing.space4) {
                            Text(dongName)
                                .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                                .foregroundStyle(colors.text.primary)
                            if let tagLabel {
                                Text(tagLabel)
                                    .runvealTextStyle(RunvealTheme.typography.captionSRegular)
                                    .foregroundStyle(colors.text.tertiary)
                            }
                        }
                        StatValue(
                            value: "\(ratePercent)",
                            unit: RunvealDesignSystemStrings.percentUnit,
                            numberStyle: RunvealTheme.typography.numDisplay,
                            unitStyle: RunvealTheme.typography.unitDisplay,
                            numberColor: colors.text.primary,
                            unitColor: colors.text.secondary,
                            gap: 0
                        )
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    RunvealDongThumbnail(content: thumbnail)
                }
                .frame(maxWidth: .infinity)
                .contentShape(RunvealShapes.radius24)
            }
            .buttonStyle(.plain)

            if let onChevronClick {
                Button(action: onChevronClick) {
                    RunvealIcon(RunvealIcons.chevron, tint: colors.icon.tertiary)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(RunvealDesignSystemStrings.summaryCardZoomIn)
            }
        }
        .padding(.horizontal, RunvealLayout.marginCardPadding)
        .padding(.vertical, RunvealSpacing.space12)
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    RunvealSummaryCard(dongName: "성수동", ratePercent: 37, isMine: true) {}
        .background(RunvealTheme.colors.bg.canvas)
}
