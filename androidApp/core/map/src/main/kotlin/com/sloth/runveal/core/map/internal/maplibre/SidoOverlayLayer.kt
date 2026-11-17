package com.sloth.runveal.core.map.internal.maplibre

import androidx.compose.ui.graphics.toArgb
import com.sloth.runveal.core.map.SidoOverlayGeoJson
import com.sloth.runveal.designsystem.map.RunvealDong
import com.sloth.runveal.map.MapConfig
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
 * 똑같이 MapLibre 레이어 자체의 maxZoom으로 전환한다. fill은 안 만든다(경계선 + 이름만). 테두리 색은
 * [SigunguOverlayLayer]와 같은 이유로 [RunvealDong] 단계별 색 토큰을 그대로 쓴다 — 소속 시군구들의
 * 완주율을 한 번 더 집계한 값([com.sloth.runveal.core.map.DongAssetRepository]).
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
                PropertyFactory.lineColor(levelColorStep { level -> RunvealDong.style(level).stroke.toArgb() }),
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
