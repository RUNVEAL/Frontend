import SwiftUI

/// 더보기(⋯) 메뉴 패널. 글래스 없이 불투명. [content]에 [RunvealMenuItem]을 나열한다.
struct RunvealMenu<Content: View>: View {
    private static var width: CGFloat { 184 }

    @ViewBuilder private let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: 0) {
            content()
        }
        .padding(RunvealSpacing.space8)
        .frame(width: Self.width)
        .background(colors.surface.raised, in: RunvealShapes.radius16)
        .overlay { RunvealShapes.radius16.stroke(colors.border.default, lineWidth: 1) }
    }
}

enum RunvealMenuItemType { case `default`, danger }

/// 메뉴 한 줄. 아이콘 20 + 라벨.
struct RunvealMenuItem: View {
    private let label: String
    private let icon: RunvealIconSpec
    private let type: RunvealMenuItemType
    private let onClick: () -> Void

    init(
        label: String,
        icon: RunvealIconSpec,
        type: RunvealMenuItemType = .default,
        onClick: @escaping () -> Void
    ) {
        self.label = label
        self.icon = icon
        self.type = type
        self.onClick = onClick
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let contentColor = type == .danger ? colors.status.error : colors.text.primary

        return Button(action: onClick) {
            HStack(spacing: RunvealSpacing.space16) {
                RunvealIcon(icon, tint: contentColor, size: 20)
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(contentColor)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, RunvealSpacing.space8)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
        }
        .runvealPressOverlayStyle(shape: RunvealShapes.radius8, overlay: colors.state.pressedOnSurface)
    }
}

#Preview {
    RunvealMenu {
        RunvealMenuItem(label: "이름 수정", icon: RunvealIcons.edit) {}
        RunvealMenuItem(label: "GPX 내보내기", icon: RunvealIcons.download) {}
        RunvealMenuItem(label: "삭제", icon: RunvealIcons.delete, type: .danger) {}
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
