import SwiftUI

/// `.sheet`으로 띄우는 바텀시트의 공통 설정.
///
/// **시트 높이를 내용에 맞춘다.** [RunvealBottomSheet]는 내용만큼만 높이를 갖는데, `.sheet`은
/// detent가 정한 높이(지정이 없으면 화면 전체)를 그대로 잡는다. 그러면 카드 위쪽에 **보이지 않는
/// 시트 영역**이 남아서,
/// - 카드가 바닥에서 떠 보이고,
/// - 그 영역이 시트의 일부라 뒤(지도)를 눌러도 아무 일도 안 일어나며,
/// - 시스템 딤(scrim)이 생기지 않아 바깥을 눌러 닫을 수도 없다.
///
/// 높이를 내용에 맞추면 보이는 카드가 곧 시트가 되어, 카드를 끌어내려 닫기·바깥 눌러 닫기가
/// 시스템 동작 그대로 작동한다. Android `ModalBottomSheet(dragHandle = null,
/// skipPartiallyExpanded = true)`와 같은 모양이다 — 중간 단계가 없으니 핸들로 크기를 "조절"하는
/// 동작은 양쪽 다 없고, 핸들은 카드가 직접 그리는 표시다.
extension View {
    func runvealSheetContent() -> some View {
        modifier(RunvealSheetContentModifier())
    }
}

private struct RunvealSheetContentModifier: ViewModifier {
    /// 잰 내용 높이 — 시트 높이를 여기에 맞춘다. 재기 전(0)에는 잠깐 절반 높이로 띄운다.
    @State private var contentHeight: CGFloat = 0

    func body(content: Content) -> some View {
        content
            // 제안 높이와 무관하게 **내용의 원래 높이**를 쓰게 한다 — 그래야 detent를 따라
            // 늘었다 줄었다 하지 않고 한 번에 맞는다.
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity)
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .onAppear { contentHeight = geometry.size.height }
                        .onChange(of: geometry.size.height) { _, height in contentHeight = height }
                }
            )
            .presentationDetents(contentHeight > 0 ? [.height(contentHeight)] : [.medium])
            .presentationBackground(.clear)
            .presentationDragIndicator(.hidden)
    }
}
