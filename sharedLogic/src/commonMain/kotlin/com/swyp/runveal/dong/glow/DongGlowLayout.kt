package com.swyp.runveal.dong.glow

import kotlin.math.ceil
import kotlin.math.max

/** 동 하나(Mercator 미터 bbox)를 몇 px 이미지로 그릴지 정한 배치 정보. */
internal data class DongGlowLayout(
    val quality: Float,
    val pxPerM: Double,
    val effScale: Double,
    val marginPx: Int,
    val widthPx: Int,
    val heightPx: Int,
    val minX: Double,
    val maxY: Double,
)

/** 문서 3-2-3. `blur`가 클수록 margin이 커지고, 그래도 [DongGlowSpec.MAX_SIDE]를 넘으면 quality를 줄여 다시 잰다. */
internal object DongGlowLayoutCalculator {
    fun sigma(blurPx: Float, effScale: Double): Double = max(blurPx * effScale / 2.0, 0.001)

    fun compute(minX: Double, minY: Double, maxX: Double, maxY: Double, inradiusM: Double, maxBlurPx: Float): DongGlowLayout {
        var q = DongGlowSpec.QUALITY.toDouble()
        while (true) {
            val pxPerM = DongGlowSpec.REF_INRADIUS_PX * q / inradiusM
            val effScale = DongGlowSpec.REF_EFFECT_SCALE * q
            val margin = ceil(3 * sigma(maxBlurPx, effScale)).toInt() + 2
            val w = ceil((maxX - minX) * pxPerM).toInt() + 2 * margin
            val h = ceil((maxY - minY) * pxPerM).toInt() + 2 * margin
            val side = max(w, h)
            if (side > DongGlowSpec.MAX_SIDE) {
                q *= DongGlowSpec.MAX_SIDE / side.toDouble() * 0.98
                continue
            }
            return DongGlowLayout(
                quality = q.toFloat(),
                pxPerM = pxPerM,
                effScale = effScale,
                marginPx = margin,
                widthPx = w,
                heightPx = h,
                minX = minX,
                maxY = maxY,
            )
        }
    }
}

/** [DongGlowLayout]의 네 꼭짓점(좌상,우상,우하,좌하)을 Mercator 미터로 돌려준다. [DongGlowCorners]가 위경도로 바꾼다. */
internal fun DongGlowLayout.cornersMeters(): List<Vec2> {
    val x0 = minX - marginPx / pxPerM
    val x1 = x0 + widthPx / pxPerM
    val y1 = maxY + marginPx / pxPerM
    val y0 = y1 - heightPx / pxPerM
    return listOf(Vec2(x0, y1), Vec2(x1, y1), Vec2(x1, y0), Vec2(x0, y0))
}
