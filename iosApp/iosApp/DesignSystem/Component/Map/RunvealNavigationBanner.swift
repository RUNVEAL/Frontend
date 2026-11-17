import SwiftUI

/// 러닝 중 길 안내 배너. [icon]은 RunvealIcons.turnLeft / turnRight / pause 중 하나를 문구와 함께
/// 바꿔 전달한다. 보조 정보가 없으면 [description]을 생략한다.
struct RunvealNavigationBanner: View {
    private let icon: RunvealIconSpec
    private let instruction: String
    private let description: String?

    init(icon: RunvealIconSpec, instruction: String, description: String? = nil) {
        self.icon = icon
        self.instruction = instruction
        self.description = description
    }

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space12) {
            RunvealIcon(icon, tint: colors.icon.primary, size: 32)
            VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                Text(instruction)
                    .runvealTextStyle(RunvealTheme.typography.heading3)
                    .foregroundStyle(colors.text.primary)
                if let description {
                    Text(description)
                        .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(RunvealSpacing.space16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .runvealGlass(RunvealShapes.radius16)
    }
}

#Preview {
    RunvealNavigationBanner(icon: RunvealIcons.turnLeft, instruction: "좌회전", description: "50m 앞")
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
