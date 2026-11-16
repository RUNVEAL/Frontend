import SwiftUI

/// Close=흐름 종료(러닝 결과), Back=하위 화면, None=왼쪽 아이콘 없음(탭 첫 화면).
enum RunvealTopAppBarLeading { case close, back, none }

/// Default=일반 화면, Map=지도 위 화면용(왼쪽 버튼이 글래스 원형 Icon Button M).
enum RunvealTopAppBarStyle { case `default`, map }

/// 상단 앱바. 배경 투명(화면 위에 얹음). 오른쪽 Text 버튼(건너뛰기 등)은 [actionLabel]이 있을 때만 보인다.
struct RunvealTopAppBar: View {
    private let leading: RunvealTopAppBarLeading
    private let style: RunvealTopAppBarStyle
    private let leadingAccessibilityLabel: String?
    private let title: String?
    private let actionLabel: String?
    private let onLeadingClick: () -> Void
    private let onActionClick: () -> Void

    init(
        leading: RunvealTopAppBarLeading,
        style: RunvealTopAppBarStyle = .default,
        leadingAccessibilityLabel: String? = nil,
        title: String? = nil,
        actionLabel: String? = nil,
        onLeadingClick: @escaping () -> Void,
        onActionClick: @escaping () -> Void = {}
    ) {
        self.leading = leading
        self.style = style
        self.leadingAccessibilityLabel = leadingAccessibilityLabel
        self.title = title
        self.actionLabel = actionLabel
        self.onLeadingClick = onLeadingClick
        self.onActionClick = onActionClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let leadingIcon: RunvealIconSpec? = {
            switch leading {
            case .close: return RunvealIcons.close
            case .back: return RunvealIcons.back
            case .none: return nil
            }
        }()
        let paddingStart: CGFloat
        let paddingEnd: CGFloat
        let gap: CGFloat
        if style == .map {
            paddingStart = RunvealSpacing.space16
            paddingEnd = RunvealSpacing.space16
            gap = RunvealSpacing.space12
        } else if leading == .none {
            paddingStart = RunvealSpacing.space16
            paddingEnd = RunvealSpacing.space4
            gap = RunvealSpacing.space4
        } else {
            paddingStart = RunvealSpacing.space4
            paddingEnd = RunvealSpacing.space16
            gap = RunvealSpacing.space4
        }

        return HStack(spacing: 0) {
            if let leadingIcon {
                if style == .map {
                    RunvealIconButton(
                        icon: leadingIcon,
                        accessibilityLabel: leadingAccessibilityLabel,
                        size: .m,
                        onClick: onLeadingClick
                    )
                } else {
                    Button(action: onLeadingClick) {
                        RunvealIcon(leadingIcon, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
                            .frame(width: RunvealLayout.sizeTouchTargetMin, height: RunvealLayout.sizeTouchTargetMin)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(leadingAccessibilityLabel ?? "")
                }
                Spacer().frame(width: gap)
            }
            if let title {
                Text(title)
                    .runvealTextStyle(RunvealTheme.typography.heading3)
                    .foregroundStyle(colors.text.primary)
                if actionLabel != nil { Spacer(minLength: 0) }
            } else if actionLabel != nil {
                Spacer(minLength: 0)
            }
            if let actionLabel {
                RunvealButton(label: actionLabel, style: .text, onClick: onActionClick)
            }
        }
        .padding(.leading, paddingStart)
        .padding(.trailing, paddingEnd)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: RunvealLayout.sizeTopAppBar)
    }
}

#Preview {
    VStack(spacing: 0) {
        RunvealTopAppBar(leading: .close, title: "러닝 완료!") {}
        RunvealTopAppBar(leading: .back, style: .map, actionLabel: "건너뛰기", onLeadingClick: {}, onActionClick: {})
    }
    .background(RunvealTheme.colors.bg.canvas)
}
