package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection

/**
 * 가본 길 선 레이어 — 소스 1개 + LineLayer(코어, [glowColor]를 주면 글로우 2개 더). [sourceId]를
 * 호출하는 쪽이 넘겨서, 홈 지도의 누적 "가본 길"과 러닝 중 실시간 선(이미 가본 구간/새 구간)이 서로
 * 다른 소스·레이어를 쓰게 한다 — 레이어 ID는 소스 ID에서 그대로 파생시킨다([RecommendedRouteLayer]와
 * 같은 패턴). 블러·두께·줌 보간 값은 토큰에 없는 MapLibre 전용 튜닝이라 여기 상수로 유지한다.
 * [MapConfig.ZOOM_IN_THRESHOLD](줌인 러닝 포커스 전환 기준)보다 축소하면 숨긴다 — 그보다 축소된
 * 뷰는 동/시군구/시도 완주율 테두리를 보여주는 용도라 길 하나하나가 겹쳐 지저분해 보인다.
 */
internal object VisitedRoadLayer {
    private const val GLOW_WIDTH_DP = 16f
    private const val GLOW_OPACITY = 0.45f
    private const val GLOW_BLUR_DP = 14f

    private const val CORE_WIDTH_DP = 3f
    private const val CORE_OPACITY = 1f

    private const val ZOOM_SMALL = 12.0
    private const val ZOOM_BASE = 16.0
    private const val SMALL_SCALE = 0.4f

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, sourceId: String, geoJson: String?, coreColor: Int, glowColor: Int? = null) {
        var source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            source = GeoJsonSource(sourceId)
            style.addSource(source)
            ensureLayers(style, sourceId, coreColor, glowColor)
        }
        source.setGeoJson(geoJson ?: EMPTY_FEATURE_COLLECTION_JSON)
    }

    private fun ensureLayers(style: Style, sourceId: String, coreColor: Int, glowColor: Int?) {
        if (glowColor != null) {
            addLineLayerIfMissing(style, sourceId, "$sourceId-glow-outer", glowColor, GLOW_WIDTH_DP, GLOW_OPACITY, GLOW_BLUR_DP)
            addLineLayerIfMissing(style, sourceId, "$sourceId-glow-mid", glowColor, GLOW_WIDTH_DP / 2, GLOW_OPACITY, GLOW_BLUR_DP / 2)
        }
        addLineLayerIfMissing(style, sourceId, "$sourceId-line", coreColor, CORE_WIDTH_DP, CORE_OPACITY, 0f)
    }

    private fun addLineLayerIfMissing(style: Style, sourceId: String, layerId: String, colorInt: Int, widthDp: Float, opacity: Float, blurDp: Float) {
        if (style.getLayer(layerId) != null) return
        val layer = LineLayer(layerId, sourceId).withProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineColor(colorInt),
            PropertyFactory.lineWidth(zoomInterpolated(widthDp)),
            PropertyFactory.lineOpacity(opacity),
            PropertyFactory.lineBlur(zoomInterpolated(blurDp)),
        )
        layer.minZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
        style.addLayer(layer)
    }

    private fun zoomInterpolated(baseDp: Float): Expression = Expression.interpolate(
        Expression.linear(),
        Expression.zoom(),
        Expression.stop(ZOOM_SMALL, Expression.literal(baseDp * SMALL_SCALE)),
        Expression.stop(ZOOM_BASE, Expression.literal(baseDp)),
    )
}
