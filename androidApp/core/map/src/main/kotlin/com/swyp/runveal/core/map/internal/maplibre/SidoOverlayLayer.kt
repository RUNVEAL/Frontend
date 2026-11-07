package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.core.map.SidoOverlayGeoJson
import com.swyp.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection

/**
 * 시도(광역시·특별시·도) 테두리/라벨 — [MapConfig.SIDO_SWITCH_ZOOM]보다 축소(줌아웃)했을 때만 보이고,
 * 그 이상 확대하면 [SigunguOverlayLayer]의 시군구 레이어로 자동 전환된다. [SigunguOverlayLayer]와
 * 똑같이 MapLibre 레이어 자체의 maxZoom으로 전환한다. 완주율 개념이 없어 fill은 안 만든다
 * (경계선 + 이름만) — 색은 시군구 레이어와 같은 토큰([MapColors.sigunguBorder]/[MapColors.sigunguLabel])을
 * 그대로 쓴다. 경계 "레벨"이 다를 뿐 시각적으로는 같은 종류의 정보라 별도 색을 두지 않았다.
 */
internal object SidoOverlayLayer {
    private const val LINE_WIDTH_DP = 2f
    private const val LABEL_SIZE_SP = 13f
    private val LABEL_FONT = arrayOf("Noto Sans Regular")

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, overlay: SidoOverlayGeoJson?, colors: MapColors) {
        ensureSource(style, MapConfig.SOURCE_SIDO_LINE, overlay?.lineGeoJson)
        ensureSource(style, MapConfig.SOURCE_SIDO_LABEL, overlay?.labelGeoJson)
        ensureLayers(style, colors)
    }

    private fun ensureSource(style: Style, sourceId: String, geoJson: String?) {
        var source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            source = GeoJsonSource(sourceId)
            style.addSource(source)
        }
        source.setGeoJson(geoJson ?: EMPTY_FEATURE_COLLECTION_JSON)
    }

    private fun ensureLayers(style: Style, colors: MapColors) {
        if (style.getLayer(MapConfig.LAYER_SIDO_LINE) == null) {
            val layer = LineLayer(MapConfig.LAYER_SIDO_LINE, MapConfig.SOURCE_SIDO_LINE).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineColor(colors.sigunguBorder),
                PropertyFactory.lineWidth(LINE_WIDTH_DP),
            )
            layer.maxZoom = MapConfig.SIDO_SWITCH_ZOOM.toFloat()
            style.addLayer(layer)
        }
        if (style.getLayer(MapConfig.LAYER_SIDO_LABEL) == null) {
            val layer = SymbolLayer(MapConfig.LAYER_SIDO_LABEL, MapConfig.SOURCE_SIDO_LABEL).withProperties(
                PropertyFactory.textField(Expression.get("name")),
                PropertyFactory.textFont(LABEL_FONT),
                PropertyFactory.textSize(LABEL_SIZE_SP),
                PropertyFactory.textColor(colors.sigunguLabel),
                PropertyFactory.textJustify(Property.TEXT_JUSTIFY_CENTER),
                // 17개뿐인 고정 목록이라 겹침 방지로 일부를 숨길 이유가 없다 — 항상 전부 보이게 한다.
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true),
            )
            layer.maxZoom = MapConfig.SIDO_SWITCH_ZOOM.toFloat()
            style.addLayer(layer)
        }
    }
}
