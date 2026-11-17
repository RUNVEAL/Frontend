package com.sloth.runveal.ios

import com.sloth.runveal.dong.DongPolygonPart
import com.sloth.runveal.dong.glow.DongGlowRenderer
import com.sloth.runveal.dong.glow.GlowEffect
import com.sloth.runveal.map.LatLon
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * 동 빛 이미지 한 장 — 8비트 RGBA(straight alpha) 픽셀과 네 꼭짓점(위경도).
 *
 * 픽셀을 [NSData]로 넘기는 게 핵심이다. `DongGlowRenderer.RenderResult.rgba`는 `FloatArray`인데,
 * Swift에서 `KotlinFloatArray`를 인덱스로 하나씩 읽으면 픽셀 수 × 4번 브리지를 넘나들어서(1000x1000
 * 이면 400만 번) 쓸 수 없다. 여기서 바이트로 눌러 담아 한 덩어리로 건네면 Swift는 `CGDataProvider`로
 * 곧바로 `CGImage`를 만들 수 있다.
 */
@OptIn(ExperimentalForeignApi::class)
class DongGlowImage internal constructor(
    val width: Int,
    val height: Int,
    /** `width * height * 4` 바이트, R,G,B,A 순서(straight alpha). */
    val rgba: NSData,
    /** [좌상, 우상, 우하, 좌하] 순서. */
    val corners: List<LatLon>,
)

/**
 * [DongGlowRenderer](commonMain, `tools/dong-glow/gen_dong_glow.py` 포팅)를 iOS에서 쓰기 좋은 모양으로
 * 감싼다 — Android `core:map`의 `DongGlowBitmap`(`RenderResult` → `Bitmap`)에 해당한다.
 *
 * 효과 값([GlowEffect])은 디자인 토큰에서 나오는데 iOS 쪽 토큰은 Swift(`RunvealEffects.dong1`~`dong6`,
 * `RunvealLayout.mapDongLevelStrokeWidth0`~`6`)에 있으므로, Swift가 만들어서 넘긴다
 * (Android는 `DongGlowEffects`가 Kotlin 토큰에서 만든다).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
object IosDongGlowRenderer {

    fun render(
        parts: List<DongPolygonPart>,
        strokeWidthPx: Float,
        effects: List<GlowEffect>,
    ): DongGlowImage {
        val result = DongGlowRenderer.render(parts, strokeWidthPx, effects)
        return DongGlowImage(
            width = result.width,
            height = result.height,
            rgba = result.rgba.toRgbaBytes().toNSData(),
            corners = result.corners,
        )
    }

    /** 채널 0..1 Float → 0..255 바이트. `Bitmap.setPixels`와 같은 straight alpha 그대로 둔다. */
    private fun FloatArray.toRgbaBytes(): ByteArray {
        val bytes = ByteArray(size)
        for (i in indices) {
            val scaled = (this[i] * 255f + 0.5f).toInt().coerceIn(0, 255)
            bytes[i] = scaled.toByte()
        }
        return bytes
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}
