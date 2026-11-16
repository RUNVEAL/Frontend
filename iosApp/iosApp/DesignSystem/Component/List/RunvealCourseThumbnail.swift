import SwiftUI

/// 코스 썸네일 72. 안쪽 지도와 경로는 [content] 슬롯(앱이 직접 그리거나 서버 이미지)으로 그린다.
enum RunvealCourseThumbnailType { case oneWay, circulation }

struct RunvealCourseThumbnail<Content: View>: View {
    private static var side: CGFloat { 72 }

    private let type: RunvealCourseThumbnailType
    @ViewBuilder private let content: () -> Content

    init(type: RunvealCourseThumbnailType, @ViewBuilder content: @escaping () -> Content = { EmptyView() }) {
        self.type = type
        self.content = content
    }

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack {
            content()

            switch type {
            case .oneWay:
                RunvealMapMarker(type: .start, size: .small)
                    .padding(RunvealSpacing.space4)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                RunvealMapMarker(type: .end, size: .small)
                    .padding(RunvealSpacing.space4)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
            case .circulation:
                RunvealMapMarker(type: .start, size: .small)
            }
        }
        .frame(width: Self.side, height: Self.side)
        .background(colors.bg.map, in: RunvealShapes.radius8)
        .overlay { RunvealShapes.radius8.stroke(colors.border.subtle, lineWidth: 1) }
        .clipShape(RunvealShapes.radius8)
    }
}

#Preview {
    HStack(spacing: 12) {
        RunvealCourseThumbnail(type: .oneWay)
        RunvealCourseThumbnail(type: .circulation)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
