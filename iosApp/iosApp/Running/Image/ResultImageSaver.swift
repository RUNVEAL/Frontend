import Photos
import SwiftUI
import UIKit

/**
 결과 이미지를 그려서 **사진 보관함에 저장**한다. Android `GalleryImageSaver` + `ComposeImageCapture`에
 해당한다.

 **앨범을 따로 만들지 않는다.** Android는 `Pictures/Runveal` 폴더에 넣지만, iOS에서 앨범을 만들려면
 추가 전용(`.addOnly`)이 아니라 보관함 **읽기** 권한까지 받아야 한다 — 사진을 한 장 넣자고 남의 사진을
 다 볼 권한을 요구하는 꼴이라, 추가 전용 권한으로 보관함에만 넣는다.

 실패는 예외로 던지지 않고 [Outcome]으로 알린다. 저장이 안 되는 건 앱을 멈출 일이 아니라 "저장하지
 못했어요"만 알리면 되는 상황이다.
 */
enum ResultImageSaver {
    enum Outcome {
        case saved
        case failed
        /// 사용자가 사진 추가를 거부했다 — 설정에서 바꿔야 해서 안내 문구가 다르다.
        case permissionDenied
    }

    /**
     SwiftUI 뷰를 [scale]배 크기의 비트맵으로 그린다.

     화면에 붙어 있는 뷰가 아니라 **따로 만든 템플릿**을 그린다(`ResultShareImage`) — 기기 화면 크기와
     무관하게 늘 같은 그림이어야 하고, 스크롤로 가려진 부분도 다 들어가야 한다.
     */
    @MainActor
    static func render(_ content: some View, scale: CGFloat) -> UIImage? {
        let renderer = ImageRenderer(content: content)
        renderer.scale = scale
        // 템플릿이 자기 배경을 칠하지만 렌더러 기본은 투명이다 — PNG로 나갈 때 가장자리가 비지 않게 둔다.
        renderer.isOpaque = true
        return renderer.uiImage
    }

    /// 보관함에 넣는다. 권한은 **추가 전용**으로만 묻는다.
    static func save(_ image: UIImage) async -> Outcome {
        let status = await requestAddOnlyAuthorization()
        // `.limited`는 "고른 사진만" 상태인데, 추가 전용에서는 넣는 것 자체는 된다.
        guard status == .authorized || status == .limited else { return .permissionDenied }

        do {
            try await PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            }
            return .saved
        } catch {
            return .failed
        }
    }

    /// 이미 정해진 권한은 다시 묻지 않는다 — 거부한 뒤 또 물으면 시스템이 그냥 거부로 답한다.
    private static func requestAddOnlyAuthorization() async -> PHAuthorizationStatus {
        let current = PHPhotoLibrary.authorizationStatus(for: .addOnly)
        guard current == .notDetermined else { return current }
        return await PHPhotoLibrary.requestAuthorization(for: .addOnly)
    }
}
