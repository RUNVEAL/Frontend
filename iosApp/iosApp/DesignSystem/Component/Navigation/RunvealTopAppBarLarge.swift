import SwiftUI

/// 탭 첫 화면의 큰 제목(내 코스, 기록 등). 상태바 바로 아래.
/// [onMetaLabelClick]을 주면 metaLabel(+subtitle) 옆에 expand 아이콘이 붙고 탭할 수 있게 된다
/// (기록 탭의 월 선택처럼). 안 주면(기본) 그냥 텍스트다.
struct RunvealTopAppBarLarge<Action: View>: View {
    private let title: String
    private let metaLabel: String
    private let titleStyle: RunvealTextStyle
    private let subtitle: String?
    private let onMetaLabelClick: (() -> Void)?
    @ViewBuilder private let action: () -> Action

    init(
        title: String,
        metaLabel: String,
        titleStyle: RunvealTextStyle = RunvealTheme.typography.heading1Semibold,
        subtitle: String? = nil,
        onMetaLabelClick: (() -> Void)? = nil,
        @ViewBuilder action: @escaping () -> Action = { EmptyView() }
    ) {
        self.title = title
        self.metaLabel = metaLabel
        self.titleStyle = titleStyle
        self.subtitle = subtitle
        self.onMetaLabelClick = onMetaLabelClick
        self.action = action
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space12) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                Text(title)
                    .runvealTextStyle(titleStyle)
                    .foregroundStyle(colors.text.primary)

                let meta = HStack(spacing: RunvealSpacing.space4) {
                    Text(metaLabel)
                        .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                        .foregroundStyle(colors.text.secondary)
                    if let subtitle {
                        Text(subtitle)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                    }
                    if onMetaLabelClick != nil {
                        RunvealIcon(RunvealIcons.expand, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
                    }
                }

                if let onMetaLabelClick {
                    Button(action: onMetaLabelClick) {
                        meta.contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                } else {
                    meta
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            action()
        }
        .padding(.vertical, RunvealSpacing.space12)
        .padding(.horizontal, RunvealSpacing.space16)
        .frame(maxWidth: .infinity)
        .frame(height: 84)
    }
}

#Preview {
    RunvealTopAppBarLarge(
        title: "내 코스",
        metaLabel: "저장한 코스 4개",
        subtitle: "| 아직 안 달린 코스 2개"
    ) {
        RunvealIconButton(icon: RunvealIcons.plus, accessibilityLabel: "코스 추가", size: .m) {}
    }
    .background(RunvealTheme.colors.bg.canvas)
}
