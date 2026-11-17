import SwiftUI

/// 하단 시트 내용(손잡이 → 머리 → 내용 → 버튼). 뒤 bg/scrim과 보이기/숨기기, 드래그 닫기는
/// 호출하는 쪽의 시트 호스트(`.sheet` / `.overlay` 등)가 맡는다 — 이 뷰는 시트 모양만 그린다.
/// 내용이 길면 [content]를 스크롤 가능한 컨테이너로 감싸서 전달한다.
struct RunvealBottomSheet<Header: View, Content: View, Footer: View>: View {
    @ViewBuilder private let header: () -> Header
    @ViewBuilder private let content: () -> Content
    @ViewBuilder private let footer: () -> Footer

    init(
        @ViewBuilder header: @escaping () -> Header = { EmptyView() },
        @ViewBuilder content: @escaping () -> Content,
        @ViewBuilder footer: @escaping () -> Footer = { EmptyView() }
    ) {
        self.header = header
        self.content = content
        self.footer = footer
    }

    var body: some View {
        let colors = RunvealTheme.colors
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: RunvealRadius.radius24,
            bottomLeadingRadius: 0,
            bottomTrailingRadius: 0,
            topTrailingRadius: RunvealRadius.radius24
        )
        let hasFooter = !(Footer.self == EmptyView.self)

        VStack(spacing: 0) {
            RunvealShapes.full
                .fill(colors.border.default)
                .frame(width: 36, height: 4)

            VStack(alignment: .leading, spacing: RunvealSpacing.space20) {
                if !(Header.self == EmptyView.self) { header() }
                content()
            }
            .padding(.horizontal, RunvealLayout.marginScreen)
            .padding(.top, RunvealSpacing.space20)
            .frame(maxWidth: .infinity, alignment: .leading)

            if hasFooter {
                Spacer().frame(height: RunvealSpacing.space20)
                footer()
            } else {
                Spacer().frame(height: RunvealSpacing.space32)
            }
        }
        .padding(.top, RunvealSpacing.space12)
        .frame(maxWidth: .infinity)
        .background(colors.surface.raised, in: shape)
        .clipShape(shape)
    }
}

#Preview {
    RunvealBottomSheet(
        header: {
            RunvealBottomSheetHeader(type: .title, title: "코스 정렬")
        },
        content: {
            VStack(alignment: .leading) {
                Text("거리순").foregroundStyle(RunvealTheme.colors.text.primary)
                Text("최신순").foregroundStyle(RunvealTheme.colors.text.primary)
            }
        },
        footer: {
            RunvealButtonGroup {
                RunvealButtonGroupSingle(label: "적용") {}
            }
        }
    )
    .background(RunvealTheme.colors.bg.canvas)
}
