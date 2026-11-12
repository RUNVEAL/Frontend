package com.swyp.runveal.core.map.internal.maplibre

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection

/**
 * 추천 코스 레이어 — 소스 1개 + LineLayer 3개(선택 안 된 코스, 선택된 코스 글로우, 선택된 코스 코어).
 * 피처의 `selected` 속성으로 필터링해서 선택된 코스만 강조한다. 두께·블러는 [VisitedRoadLayer]처럼
 * 토큰에 없는 MapLibre 전용 튜닝이라 여기 상수로 유지한다.
 *
 * [sourceId]를 호출하는 쪽이 넘긴다 — 코스 추천 결과(여러 코스)와 코스 상세(코스 하나)가 서로 다른
 * 소스를 쓰게 해서, 두 화면의 Compose 생명주기가 전환 시점에 겹쳐도 한쪽의 레이어 정리가 다른 쪽이
 * 막 그린 선을 지워버리는 경합이 생기지 않게 한다. 레이어 ID는 소스 ID에서 그대로 파생시킨다.
 */
internal object RecommendedRouteLayer {
    private const val ALT_WIDTH_DP = 3f
    private const val ALT_OPACITY = 0.7f

    private const val GLOW_WIDTH_DP = 12f
    private const val GLOW_OPACITY = 0.5f
    private const val GLOW_BLUR_DP = 10f

    private const val CORE_WIDTH_DP = 4f

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, sourceId: String, routeGeoJson: String?, colors: MapColors) {
        var source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            source = GeoJsonSource(sourceId)
            style.addSource(source)
            ensureLayers(style, sourceId, colors)
        }
        source.setGeoJson(routeGeoJson ?: EMPTY_FEATURE_COLLECTION_JSON)
    }

    private fun ensureLayers(style: Style, sourceId: String, colors: MapColors) {
        addLineLayerIfMissing(
            style = style,
            sourceId = sourceId,
            layerId = "$sourceId-alt",
            colorInt = colors.recommendedRouteAlt,
            widthDp = ALT_WIDTH_DP,
            opacity = ALT_OPACITY,
            blurDp = 0f,
            selected = false,
        )
        addLineLayerIfMissing(
            style = style,
            sourceId = sourceId,
            layerId = "$sourceId-selected-glow",
            colorInt = colors.recommendedRouteSelectedGlow,
            widthDp = GLOW_WIDTH_DP,
            opacity = GLOW_OPACITY,
            blurDp = GLOW_BLUR_DP,
            selected = true,
        )
        addLineLayerIfMissing(
            style = style,
            sourceId = sourceId,
            layerId = "$sourceId-selected",
            colorInt = colors.recommendedRouteSelected,
            widthDp = CORE_WIDTH_DP,
            opacity = 1f,
            blurDp = 0f,
            selected = true,
        )
    }

    private fun addLineLayerIfMissing(
        style: Style,
        sourceId: String,
        layerId: String,
        colorInt: Int,
        widthDp: Float,
        opacity: Float,
        blurDp: Float,
        selected: Boolean,
    ) {
        if (style.getLayer(layerId) != null) return
        val layer = LineLayer(layerId, sourceId).withProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineColor(colorInt),
            PropertyFactory.lineWidth(widthDp),
            PropertyFactory.lineOpacity(opacity),
            PropertyFactory.lineBlur(blurDp),
        )
        layer.setFilter(Expression.eq(Expression.get("selected"), Expression.literal(selected)))
        style.addLayer(layer)
    }
}
