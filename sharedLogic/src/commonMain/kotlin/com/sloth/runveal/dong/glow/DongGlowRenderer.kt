package com.sloth.runveal.dong.glow

import com.sloth.runveal.dong.DongPolygonPart
import com.sloth.runveal.map.LatLon
import kotlin.math.max

/** [DongPolygonPart](위경도)를 Mercator 미터로 투영한 버전. */
internal data class MercatorPolygonPart(val exterior: List<Vec2>, val holes: List<List<Vec2>>)

internal fun DongPolygonPart.toMercator(): MercatorPolygonPart =
    MercatorPolygonPart(
        exterior = exterior.map(Mercator::project),
        holes = holes.map { hole -> hole.map(Mercator::project) },
    )

/**
 * `tools/dong-glow/gen_dong_glow.py`를 그대로 옮긴 동 빛 이미지 생성기(문서 3-2 전체). 동 하나·단계
 * 하나의 입력(폴리곤 조각들, 테두리 두께, 효과 레이어)을 받아 RGBA 픽셀과 네 꼭짓점을 만든다. 실제
 * 비트맵 변환·캐시·지도 표시는 `core:map`(Android 전용)이 담당한다 — Mercator 투영·픽셀 래스터화 같은
 * 내부 구현은 숨기고, 공개 API는 이 진입점(이미 있는 [com.sloth.runveal.dong.DongPolygonPart] 입력)뿐이다.
 */
object DongGlowRenderer {
    data class RenderResult(
        val width: Int,
        val height: Int,
        /** straight alpha RGBA, 채널마다 0..1. 크기 = `width * height * 4`. */
        val rgba: FloatArray,
        /** [좌상, 우상, 우하, 좌하] 순서. */
        val corners: List<LatLon>,
        val inradiusM: Double,
        val quality: Float,
    )

    /**
     * [inradiusOverrideM]은 골든 테스트 진단용 — 폴리라벨(내접원) 정밀도와 배치·마스크·블러·합성 쪽을
     * 분리해서 비교할 때만 쓴다(문서 Step 2: "Python 쪽 inradius_m을 주입해서 한 번 더 비교"). 정상
     * 경로는 항상 null로 두고 폴리라벨로 직접 계산한다.
     */
    fun render(
        parts: List<DongPolygonPart>,
        strokeWidthPx: Float,
        effects: List<GlowEffect>,
        inradiusOverrideM: Double? = null,
    ): RenderResult {
        require(parts.isNotEmpty()) { "parts must not be empty" }
        require(effects.isNotEmpty()) { "effects must not be empty" }
        val mercatorParts = parts.map { it.toMercator() }

        // 3-2-2: 조각마다 폴리라벨 → 경계까지 거리, 조각 중 최댓값을 이 동의 내접원 반지름으로 쓴다.
        val inradius = inradiusOverrideM ?: run {
            var maxDistance = 0.0
            for (part in mercatorParts) {
                val precision = DongGlowPolylabel.precisionFor(part.exterior)
                val result = DongGlowPolylabel.pole(part.exterior, part.holes, precision)
                if (result.distance > maxDistance) maxDistance = result.distance
            }
            maxDistance
        }

        // 3-2-3 배치 계산은 동 전체(모든 조각의 exterior) bbox 기준.
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (part in mercatorParts) {
            for (p in part.exterior) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
        }

        val maxBlurPx = effects.maxOf { it.blur }
        val layout = DongGlowLayoutCalculator.compute(minX, minY, maxX, maxY, inradius, maxBlurPx)

        fun toPx(v: Vec2) = PxPoint(
            x = (v.x - layout.minX) * layout.pxPerM + layout.marginPx,
            y = (layout.maxY - v.y) * layout.pxPerM + layout.marginPx,
        )
        val pixelParts = mercatorParts.map { part ->
            PixelPolygonPart(
                exterior = part.exterior.map(::toPx),
                holes = part.holes.map { hole -> hole.map(::toPx) },
            )
        }

        // 3-2-4, 3-2-5: 마스크 M, 테두리 S.
        val mask = DongGlowRaster.rasterizeMask(pixelParts, layout.widthPx, layout.heightPx)
        val strokeWidthScaled = (strokeWidthPx * layout.effScale).toFloat()
        val stroke = DongGlowRaster.rasterizeStroke(pixelParts, layout.widthPx, layout.heightPx, strokeWidthScaled)

        // 3-2-6: 효과 레이어(토큰 순서 그대로 바깥 1개 → 안쪽 2개).
        val pixelCount = layout.widthPx * layout.heightPx
        val layers = effects.map { effect ->
            val sigma = DongGlowLayoutCalculator.sigma(effect.blur, layout.effScale)
            val base: FloatArray
            val padValue: Float
            val multiplierOf: (Int) -> Float
            if (!effect.inset) {
                base = FloatArray(pixelCount) { i -> max(mask[i], stroke[i]) }
                padValue = 0f
                multiplierOf = { i -> 1f - mask[i] }
            } else {
                base = FloatArray(pixelCount) { i -> 1f - mask[i] }
                padValue = 1f
                multiplierOf = { i -> mask[i] }
            }
            val blurred = GaussianBlur.blur(base, layout.widthPx, layout.heightPx, sigma, padValue)
            val alpha = FloatArray(pixelCount) { i -> (blurred[i] * effect.alpha * multiplierOf(i)).coerceIn(0f, 1f) }
            GlowLayerResult(effect.r, effect.g, effect.b, alpha)
        }

        // 3-2-7, 3-2-8: 한 장으로 합치기 + 네 꼭짓점.
        val rgba = DongGlowCompositor.composite(layers, pixelCount)
        val corners = DongGlowCorners.compute(layout)

        return RenderResult(
            width = layout.widthPx,
            height = layout.heightPx,
            rgba = rgba,
            corners = corners,
            inradiusM = inradius,
            quality = layout.quality,
        )
    }
}
