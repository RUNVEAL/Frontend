import SwiftUI

/// 코스 성격 라벨(공원, 하천길, 순환, 편도). 누를 수 없음.
struct RunvealTag: View {
    private let label: String

    init(label: String) {
        self.label = label
    }

    var body: some View {
        let colors = RunvealTheme.colors
        Text(label)
            .runvealTextStyle(RunvealTheme.typography.captionSRegular)
            .foregroundStyle(colors.text.secondary)
            .padding(.vertical, RunvealSpacing.space2)
            .padding(.horizontal, RunvealSpacing.space4)
            .background(colors.surface.tag, in: RoundedRectangle(cornerRadius: 4))
    }
}

#Preview {
    RunvealTag(label: "공원")
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
