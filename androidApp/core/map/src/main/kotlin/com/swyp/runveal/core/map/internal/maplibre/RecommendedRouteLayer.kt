package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.map.MapConfig
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
 */
internal object RecommendedRouteLayer {
    private const val ALT_WIDTH_DP = 3f
    private const val ALT_OPACITY = 0.7f

    private const val GLOW_WIDTH_DP = 12f
    private const val GLOW_OPACITY = 0.5f
    private const val GLOW_BLUR_DP = 10f

    private const val CORE_WIDTH_DP = 4f

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, routeGeoJson: String?, colors: MapColors) {
        var source = style.getSourceAs<GeoJsonSource>(MapConfig.SOURCE_RECOMMENDED_ROUTE)
        if (source == null) {
            source = GeoJsonSource(MapConfig.SOURCE_RECOMMENDED_ROUTE)
            style.addSource(source)
            ensureLayers(style, colors)
        }
        source.setGeoJson(routeGeoJson ?: EMPTY_FEATURE_COLLECTION_JSON)
    }

    private fun ensureLayers(style: Style, colors: MapColors) {
        addLineLayerIfMissing(
            style = style,
            layerId = MapConfig.LAYER_RECOMMENDED_ROUTE_ALT,
            colorInt = colors.recommendedRouteAlt,
            widthDp = ALT_WIDTH_DP,
            opacity = ALT_OPACITY,
            blurDp = 0f,
            selected = false,
        )
        addLineLayerIfMissing(
            style = style,
            layerId = MapConfig.LAYER_RECOMMENDED_ROUTE_SELECTED_GLOW,
            colorInt = colors.recommendedRouteSelectedGlow,
            widthDp = GLOW_WIDTH_DP,
            opacity = GLOW_OPACITY,
            blurDp = GLOW_BLUR_DP,
            selected = true,
        )
        addLineLayerIfMissing(
            style = style,
            layerId = MapConfig.LAYER_RECOMMENDED_ROUTE_SELECTED,
            colorInt = colors.recommendedRouteSelected,
            widthDp = CORE_WIDTH_DP,
            opacity = 1f,
            blurDp = 0f,
            selected = true,
        )
    }

    private fun addLineLayerIfMissing(
        style: Style,
        layerId: String,
        colorInt: Int,
        widthDp: Float,
        opacity: Float,
        blurDp: Float,
        selected: Boolean,
    ) {
        if (style.getLayer(layerId) != null) return
        val layer = LineLayer(layerId, MapConfig.SOURCE_RECOMMENDED_ROUTE).withProperties(
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
