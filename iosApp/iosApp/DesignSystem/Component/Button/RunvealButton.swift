import SwiftUI

/// Gold=주요 행동, Secondary=일반 버튼, Danger=삭제처럼 되돌릴 수 없는 행동, Text=배경 없는 보조 행동.
enum RunvealButtonStyle { case gold, secondary, danger, text }

/// L=하단·시트 버튼, M=화면 안 인라인 버튼(빈 상태 등, Gold/Secondary만). Text는 둘 다 같은 모양이라 영향 없음.
enum RunvealButtonSize { case l, m }

struct RunvealButton: View {
    private let label: String
    private let style: RunvealButtonStyle
    private let size: RunvealButtonSize
    private let enabled: Bool
    private let fillWidth: Bool
    private let leadingIcon: RunvealIconSpec?
    private let onClick: () -> Void

    init(
        label: String,
        style: RunvealButtonStyle = .gold,
        size: RunvealButtonSize = .l,
        enabled: Bool = true,
        fillWidth: Bool = false,
        leadingIcon: RunvealIconSpec? = nil,
        onClick: @escaping () -> Void
    ) {
        self.label = label
        self.style = style
        self.size = size
        self.enabled = enabled
        self.fillWidth = fillWidth
        self.leadingIcon = leadingIcon
        self.onClick = onClick
    }

    var body: some View {
        if style == .text {
            textButton
        } else {
            filledButton
        }
    }

    private var textButton: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            HStack(spacing: RunvealSpacing.space8) {
                if let leadingIcon {
                    RunvealIcon(leadingIcon, tint: colors.text.tertiary, size: 20)
                }
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.tertiary)
            }
            .padding(.horizontal, RunvealSpacing.space12)
            .frame(maxWidth: fillWidth ? .infinity : nil)
            .frame(minWidth: RunvealLayout.sizeTouchTargetMin, minHeight: RunvealLayout.sizeTouchTargetMin)
        }
        .runvealPressOverlayStyle(shape: RunvealShapes.full, overlay: colors.state.pressedOnSurface)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
    }

    private var filledButton: some View {
        let colors = RunvealTheme.colors
        let labelColor = Self.labelColor(for: style, colors: colors)
        let pressOverlay = style == .gold ? colors.state.pressedOnAccent : colors.state.pressedOnSurface
        let height = size == .l ? RunvealLayout.sizeButtonHeight : RunvealLayout.sizeButtonHeightM
        let minWidth: CGFloat? = size == .m ? RunvealLayout.sizeButtonMinWidthM : nil

        return Button(action: onClick) {
            let content = HStack(spacing: RunvealSpacing.space8) {
                if let leadingIcon {
                    RunvealIcon(leadingIcon, tint: labelColor, size: 20)
                }
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.labelButton)
                    .foregroundStyle(labelColor)
            }
            .padding(.horizontal, RunvealSpacing.space24)
            .frame(minWidth: minWidth, maxWidth: fillWidth ? .infinity : nil)
            .frame(height: height)

            switch style {
            case .gold:
                content.background(colors.accent.default, in: RunvealShapes.full)
            case .secondary, .danger:
                content.runvealGlass(RunvealShapes.full)
            case .text:
                content
            }
        }
        .runvealPressOverlayStyle(shape: RunvealShapes.full, overlay: pressOverlay)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
    }

    private static func labelColor(for style: RunvealButtonStyle, colors: RunvealColors) -> Color {
        switch style {
        case .gold: return colors.text.onAccent
        case .secondary: return colors.text.primary
        case .danger: return colors.status.error
        case .text: return colors.text.tertiary
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        RunvealButton(label: "러닝 시작") {}
        RunvealButton(label: "코스 만들기", style: .gold, size: .m, leadingIcon: RunvealIcons.plus) {}
        RunvealButton(label: "취소", style: .secondary) {}
        RunvealButton(label: "삭제", style: .danger) {}
        RunvealButton(label: "건너뛰기", style: .text) {}
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
