package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection

/**
 * 러닝 중("이 코스로 러닝 시작") 지도에 그리는 추천 코스 안내선 — 흰색 점선 하나뿐이라
 * [RecommendedRouteLayer]처럼 glow/selected로 여러 겹 두지 않는다.
 */
internal object CourseGuideLayer {
    private const val LINE_WIDTH_DP = 3f

    /** `lineWidth`의 배수 단위 — [대시, 간격]. 일반 점선보다 살짝 긴 대시, 간격은 촘촘하게. */
    private val LINE_DASH_PATTERN = arrayOf(1.4f, 0.5f)

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, sourceId: String, geoJson: String?, colors: MapColors) {
        var source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            source = GeoJsonSource(sourceId)
            style.addSource(source)
            ensureLayer(style, sourceId, colors)
        }
        source.setGeoJson(geoJson ?: EMPTY_FEATURE_COLLECTION_JSON)
    }

    private fun ensureLayer(style: Style, sourceId: String, colors: MapColors) {
        val layerId = "$sourceId-line"
        if (style.getLayer(layerId) != null) return
        val layer = LineLayer(layerId, sourceId).withProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_BUTT),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineColor(colors.courseGuide),
            PropertyFactory.lineWidth(LINE_WIDTH_DP),
            PropertyFactory.lineDasharray(LINE_DASH_PATTERN),
        )
        // "오늘 달린 길" 바로 위에 그린다 — Compose 효과 실행 순서에 기대지 않고 명시적으로 고정한다.
        if (style.getLayer(MapConfig.LAYER_VISITED_ROAD_LINE) != null) {
            style.addLayerAbove(layer, MapConfig.LAYER_VISITED_ROAD_LINE)
        } else {
            style.addLayer(layer)
        }
    }
}
