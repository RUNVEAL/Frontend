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
        // 내용을 먼저 둥글게 자르고 **그 뒤에** 배경을 깐다 — 순서를 바꾸면 clipShape가 아래
        // 배경의 확장분까지 잘라낸다.
        .clipShape(shape)
        // **배경만 OS 내비게이션 바(홈 인디케이터) 뒤까지 채운다** — 그래야 그 영역도 시트와 같은
        // 색으로 보인다. 내용은 safe area 위에 그대로 남으므로 버튼이 가려지지 않는다.
        // Android가 `contentWindowInsets = WindowInsets(0)`으로 컨테이너 인셋만 끄고 인셋은 시트 안
        // 버튼에만 주는 것과 같은 나눔이다.
        .background(alignment: .bottom) {
            ZStack(alignment: .bottom) {
                shape.fill(colors.surface.raised)

                // **띠를 한 번 더 아래로 내보낸다.**
                //
                // `ignoresSafeArea`만으로는 모자라다 — 그건 "컨테이너의 안전 영역까지 넓혀라"지
                // "컨테이너 밖으로 나가라"가 아니다. `.sheet`으로 띄울 때는 시트가 화면 바닥까지
                // 닿아 있어서 통하지만, 지도 위에 얹는 `runvealBottomSheet`(오버레이 호스트)에서는
                // 오버레이 자체가 이미 안전 영역 **안쪽**이라 더 내려갈 자리가 없다. 그래서 그
                // 구간이 투명하게 비친다.
                //
                // 띠는 `offset`으로 내보내므로 **레이아웃에는 영향이 없고**(시트 높이·버튼 위치
                // 그대로), 바닥까지 이미 닿은 자리에서는 화면 밖이라 보이지 않는다.
                colors.surface.raised
                    .frame(height: bottomInsetFillHeight)
                    .offset(y: bottomInsetFillHeight)
            }
            .ignoresSafeArea(edges: .bottom)
        }
    }
}

/// 시트 아래로 더 내보내는 띠의 높이 — 홈 인디케이터(제스처 바)보다 넉넉히 잡는다. 더 길어도
/// 화면 밖이라 보이지 않는다. (제네릭 타입 안에는 static 저장 프로퍼티를 둘 수 없어 파일 상수다.)
private let bottomInsetFillHeight: CGFloat = 48

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
