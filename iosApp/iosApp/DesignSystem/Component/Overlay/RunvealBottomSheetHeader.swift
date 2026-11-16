import SwiftUI

/// 바텀시트 머리. Toolbar=뒤로 + 제목 + 오른쪽 글자 행동(초기화), Title=제목 + 부제,
/// Hero=큰 아이콘 + 제목 + 설명(권한 요청 등).
enum RunvealBottomSheetHeaderType { case toolbar, title, hero }

struct RunvealBottomSheetHeader: View {
    private let type: RunvealBottomSheetHeaderType
    private let title: String
    private let subtitle: String?
    private let eyebrow: String?
    private let description: String?
    private let showBack: Bool
    private let onBackClick: () -> Void
    private let actionLabel: String?
    private let onActionClick: () -> Void
    private let heroIcon: RunvealIconSpec?

    init(
        type: RunvealBottomSheetHeaderType,
        title: String,
        subtitle: String? = nil,
        eyebrow: String? = nil,
        description: String? = nil,
        showBack: Bool = false,
        onBackClick: @escaping () -> Void = {},
        actionLabel: String? = nil,
        onActionClick: @escaping () -> Void = {},
        heroIcon: RunvealIconSpec? = nil
    ) {
        self.type = type
        self.title = title
        self.subtitle = subtitle
        self.eyebrow = eyebrow
        self.description = description
        self.showBack = showBack
        self.onBackClick = onBackClick
        self.actionLabel = actionLabel
        self.onActionClick = onActionClick
        self.heroIcon = heroIcon
    }

    var body: some View {
        let colors = RunvealTheme.colors
        switch type {
        case .toolbar:
            HStack(spacing: RunvealSpacing.space8) {
                // 바깥 패딩(marginScreen)은 콘텐츠와 공통이라, 뒤로가기 터치 영역의 여유(48-24, 좌우
                // 12씩)만큼 뒤로 아이콘+제목을 통째로 왼쪽으로 당겨서 아이콘이 다른 화면 뒤로가기
                // 버튼과 같은 16pt 지점에 오게 한다. 오른쪽 action 글자는 그대로 둔다.
                HStack(spacing: RunvealSpacing.space8) {
                    if showBack {
                        Button(action: onBackClick) {
                            RunvealIcon(RunvealIcons.back, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
                                .frame(
                                    width: RunvealLayout.sizeTouchTargetMin,
                                    height: RunvealLayout.sizeTouchTargetMin
                                )
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                    Text(title)
                        .runvealTextStyle(RunvealTheme.typography.heading3)
                        .foregroundStyle(colors.text.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .offset(
                    x: showBack
                        ? -(RunvealLayout.sizeTouchTargetMin - RunvealLayout.sizeIcon) / 2
                        : 0
                )

                if let actionLabel {
                    Button(action: onActionClick) {
                        Text(actionLabel)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.tertiary)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .frame(maxWidth: .infinity)

        case .title:
            VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                Text(title)
                    .runvealTextStyle(RunvealTheme.typography.heading3)
                    .foregroundStyle(colors.text.primary)
                if let subtitle {
                    Text(subtitle)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
            }

        case .hero:
            VStack(alignment: .leading, spacing: RunvealSpacing.space12) {
                if let heroIcon {
                    // tint를 주면 벡터의 모든 path 색이 하나로 덮어써져서, location-off 같은 두 색
                    // (전경 + 배경색 knockout) 아이콘의 의도된 모양이 깨진다. 아이콘은 이미 다크 테마
                    // 팔레트로 고정 제작돼 있어 tint가 필요 없다.
                    RunvealIcon(heroIcon, size: 32)
                }
                VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
                    if let eyebrow {
                        Text(eyebrow)
                            .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                            .foregroundStyle(colors.text.accent)
                    }
                    Text(title)
                        .runvealTextStyle(RunvealTheme.typography.heading2)
                        .foregroundStyle(colors.text.primary)
                    if let description {
                        Text(description)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 24) {
        RunvealBottomSheetHeader(
            type: .toolbar,
            title: "정렬",
            showBack: true,
            actionLabel: "초기화"
        )
        RunvealBottomSheetHeader(
            type: .hero,
            title: "위치 권한이 필요해요",
            eyebrow: "권한 요청",
            description: "내 주변 탐험 지도를 보여드릴게요",
            heroIcon: RunvealIcons.locate
        )
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
