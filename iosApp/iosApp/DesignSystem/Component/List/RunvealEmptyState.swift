import SwiftUI

/// 빈 상태. 그래픽(선택) → 제목 + 설명(가운데 정렬) → Gold 버튼 M(선택).
struct RunvealEmptyState<Graphic: View>: View {
    private let title: String
    private let description: String?
    private let actionLabel: String?
    private let onActionClick: () -> Void
    @ViewBuilder private let graphic: () -> Graphic

    init(
        title: String,
        description: String? = nil,
        actionLabel: String? = nil,
        onActionClick: @escaping () -> Void = {},
        @ViewBuilder graphic: @escaping () -> Graphic = { EmptyView() }
    ) {
        self.title = title
        self.description = description
        self.actionLabel = actionLabel
        self.onActionClick = onActionClick
        self.graphic = graphic
    }

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: RunvealSpacing.space24) {
            if !(Graphic.self == EmptyView.self) {
                ZStack { graphic() }
                    .frame(width: 80, height: 80)
            }
            VStack(spacing: RunvealSpacing.space8) {
                Text(title)
                    .runvealTextStyle(RunvealTheme.typography.heading2)
                    .foregroundStyle(colors.text.primary)
                    .multilineTextAlignment(.center)
                if let description {
                    Text(description)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            if let actionLabel {
                RunvealButton(label: actionLabel, style: .gold, size: .m, onClick: onActionClick)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    RunvealEmptyState(
        title: "아직 저장한 코스가 없어요",
        description: "원하는 코스를 만들어 보세요",
        actionLabel: "코스 만들기"
    )
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
