import SwiftUI

/// 계속 떠 있는 안내(지도를 탭해 출발지를 선택하세요). 글자 길이만큼 너비 —
/// 터치는 통과시킨다(`.allowsHitTesting(false)`).
struct RunvealHint: View {
    private let message: String

    init(message: String) {
        self.message = message
    }

    var body: some View {
        let colors = RunvealTheme.colors
        Text(message)
            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
            .foregroundStyle(colors.text.primary)
            .padding(.vertical, RunvealSpacing.space12)
            .padding(.horizontal, RunvealSpacing.space16)
            .background(colors.bg.scrim, in: RunvealShapes.radius16)
            .overlay { RunvealShapes.radius16.stroke(colors.border.subtle, lineWidth: 1) }
            .allowsHitTesting(false)
    }
}

#Preview {
    RunvealHint(message: "지도를 탭해 출발지를 선택하세요")
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
