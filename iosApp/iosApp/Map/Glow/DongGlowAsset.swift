import CoreGraphics
import Foundation
import SharedLogic
import UIKit

/// 동 빛 이미지 한 장 + 네 꼭짓점(위경도). 메모리/디스크 캐시가 같이 들고 다니는 단위 —
/// Android `DongGlowAsset`에 해당한다.
final class DongGlowAsset {
    let image: UIImage
    /// [좌상, 우상, 우하, 좌하] 순서.
    let corners: [LatLon]

    init(image: UIImage, corners: [LatLon]) {
        self.image = image
        self.corners = corners
    }

    /// 메모리 캐시 비용 계산용 — 대략적인 픽셀 바이트 수.
    var approximateByteCount: Int {
        guard let cgImage = image.cgImage else { return 0 }
        return cgImage.bytesPerRow * cgImage.height
    }

    /// sharedLogic이 만든 straight-alpha RGBA 픽셀을 `UIImage`로 바꾼다.
    ///
    /// Android는 `Bitmap.setPixels`에 straight alpha를 그대로 넣는데, CoreGraphics도
    /// `.last`(RGBA) + `premultipliedLast`가 아닌 `.last`를 쓰면 같은 의미가 된다.
    static func make(from glow: DongGlowImage) -> DongGlowAsset? {
        let width = Int(glow.width)
        let height = Int(glow.height)
        guard width > 0, height > 0 else { return nil }

        let data = glow.rgba
        guard data.length == width * height * 4 else { return nil }
        guard let provider = CGDataProvider(data: data as CFData) else { return nil }
        guard let cgImage = CGImage(
            width: width,
            height: height,
            bitsPerComponent: 8,
            bitsPerPixel: 32,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.last.rawValue),
            provider: provider,
            decode: nil,
            shouldInterpolate: true,
            intent: .defaultIntent
        ) else { return nil }

        return DongGlowAsset(image: UIImage(cgImage: cgImage), corners: glow.corners)
    }
}
