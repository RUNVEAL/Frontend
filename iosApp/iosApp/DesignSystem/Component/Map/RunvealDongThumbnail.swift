import SwiftUI

/// 동 썸네일 프레임. 내부 지도는 동마다 앱이 직접 그리므로([content] 슬롯), 여기서는
/// 배경/테두리 틀만 제공한다.
struct RunvealDongThumbnail<Content: View>: View {
    private static var side: CGFloat { 84 }

    @ViewBuilder private let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content = { EmptyView() }) {
        self.content = content
    }

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack {
            content()
        }
        .frame(width: Self.side, height: Self.side)
        .background(colors.bg.map, in: RunvealShapes.radius8)
        .overlay { RunvealShapes.radius8.stroke(colors.border.default, lineWidth: 1) }
        .clipShape(RunvealShapes.radius8)
    }
}

#Preview {
    RunvealDongThumbnail()
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
