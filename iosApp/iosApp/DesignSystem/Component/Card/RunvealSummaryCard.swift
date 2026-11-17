import SwiftUI

/// 홈 요약 카드. Home Bottom Panel 안에서만 쓴다(배경 없음) — 카드 전체 터치 시 동 상세.
struct RunvealSummaryCard: View {
    private let dongName: String
    private let ratePercent: Int
    private let isMine: Bool
    private let onClick: () -> Void

    init(dongName: String, ratePercent: Int, isMine: Bool = false, onClick: @escaping () -> Void) {
        self.dongName = dongName
        self.ratePercent = ratePercent
        self.isMine = isMine
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            HStack(spacing: RunvealSpacing.space8) {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: RunvealSpacing.space4) {
                        Text(dongName)
                            .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                            .foregroundStyle(colors.text.primary)
                        if isMine {
                            Text(RunvealDesignSystemStrings.dongMineTag)
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

                RunvealDongThumbnail()

                RunvealIcon(RunvealIcons.chevron, tint: colors.icon.tertiary, size: RunvealLayout.sizeIcon)
            }
            .padding(.horizontal, RunvealLayout.marginCardPadding)
            .padding(.vertical, RunvealSpacing.space12)
            .frame(maxWidth: .infinity)
            .contentShape(RunvealShapes.radius24)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    RunvealSummaryCard(dongName: "성수동", ratePercent: 37, isMine: true) {}
        .background(RunvealTheme.colors.bg.canvas)
}
