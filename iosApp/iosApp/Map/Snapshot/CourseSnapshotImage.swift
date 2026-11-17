import SharedLogic
import SwiftUI

/// 코스 목록/기록 목록 썸네일, 러닝 결과 카드에 쓰는 지도 스냅샷 이미지 —
/// Android `CourseSnapshotImage`에 해당한다.
///
/// 실제로 그려질 픽셀 크기를 `GeometryReader`로 직접 재서 [CourseSnapshotRenderer]에 넘긴다 —
/// 코스 목록(72pt)과 기록 목록(80pt)처럼 자리마다 크기가 달라도 호출 쪽 레이아웃을 바꿀 필요가 없다.
/// 캐시가 있으면 바로 보여주고, 없으면 플레이스홀더를 보여주며 렌더링한 뒤 전환한다. 실패하면
/// 플레이스홀더를 유지한다 — 재시도 버튼은 없고, 다음에 이 뷰가 다시 그려질 때 자동으로 재시도된다.
struct CourseSnapshotImage: View {
    private let request: SnapshotRequest?
    private let accessibilityLabel: String?

    @State private var image: UIImage?
    @State private var renderedSize: CGSize = .zero

    init(request: SnapshotRequest?, accessibilityLabel: String? = nil) {
        self.request = request
        self.accessibilityLabel = accessibilityLabel
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .accessibilityLabel(accessibilityLabel ?? "")
                } else {
                    RunvealTheme.colors.bg.map
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
            .task(id: TaskKey(request: request, size: geometry.size)) {
                await load(size: geometry.size)
            }
        }
    }

    private func load(size: CGSize) async {
        image = nil
        guard let request, size.width > 0, size.height > 0 else { return }
        let scale = UIScreen.main.scale
        let widthPx = Int(size.width * scale)
        let heightPx = Int(size.height * scale)
        guard let url = await CourseSnapshotRenderer.shared.imageURL(
            request: request,
            widthPx: widthPx,
            heightPx: heightPx
        ) else { return }
        image = UIImage(contentsOfFile: url.path)
    }

    /// `.task(id:)`가 요청/크기 중 하나라도 바뀌면 다시 돌게 하는 키.
    private struct TaskKey: Equatable {
        let ownerId: String?
        let variant: String?
        let width: Int
        let height: Int

        init(request: SnapshotRequest?, size: CGSize) {
            ownerId = request?.ownerId
            variant = request.map { "\($0.variant)" }
            width = Int(size.width)
            height = Int(size.height)
        }
    }
}
