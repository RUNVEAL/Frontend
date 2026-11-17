import SwiftUI

/// 지도 하단 코스 만들기 / 내 위치 — IconButton(L) + 아래 라벨.
struct RunvealMapAction: View {
    private let icon: RunvealIconSpec
    private let label: String
    private let onClick: () -> Void

    init(icon: RunvealIconSpec, label: String, onClick: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.onClick = onClick
    }

    var body: some View {
        VStack(spacing: RunvealSpacing.space8) {
            RunvealIconButton(icon: icon, accessibilityLabel: label, size: .l, onClick: onClick)
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                .foregroundStyle(RunvealTheme.colors.text.secondary)
        }
    }
}

#Preview {
    RunvealMapAction(icon: RunvealIcons.course, label: "코스 만들기") {}
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
