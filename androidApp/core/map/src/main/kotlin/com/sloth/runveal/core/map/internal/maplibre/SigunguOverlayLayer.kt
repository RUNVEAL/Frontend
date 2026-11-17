package com.sloth.runveal.core.map.internal.maplibre

import androidx.compose.ui.graphics.toArgb
import com.sloth.runveal.core.map.SigunguOverlayGeoJson
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
 * 시군구 테두리/라벨 — [MapConfig.SIDO_SWITCH_ZOOM]~[MapConfig.SIGUNGU_SWITCH_ZOOM] 사이에서만
 * 보인다. 그보다 확대하면 [DongOverlayLayer]의 동 레이어로, 그보다 축소하면 [SidoOverlayLayer]의
 * 시도 레이어로 자동 전환된다. 전부 MapLibre 레이어 자체의 min/maxZoom으로 전환하므로 앱 쪽에
 * 줌 리스너가 따로 없다. fill은 안 만든다(동 레벨과 같은 이유로 면은 빛 이미지가 전담). 테두리 색은
 * [RunvealDong] 단계별 색 토큰을 그대로 재사용한다 — 시군구 전용 색 토큰은 따로 없고, 소속 동들의
 * 완주율을 집계한 값([com.sloth.runveal.core.map.DongAssetRepository])이라 같은 단계 체계를 쓴다.
 */
internal object SigunguOverlayLayer {
    private const val LINE_WIDTH_DP = 1.5f
    private const val LABEL_SIZE_SP = 12f
    private val LABEL_FONT = arrayOf("Noto Sans Regular")

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, overlay: SigunguOverlayGeoJson?, colors: MapColors) {
        ensureSource(style, MapConfig.SOURCE_SIGUNGU_LINE, overlay?.lineGeoJson)
        ensureSource(style, MapConfig.SOURCE_SIGUNGU_LABEL, overlay?.labelGeoJson)
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
        if (style.getLayer(MapConfig.LAYER_SIGUNGU_LINE) == null) {
            val layer = LineLayer(MapConfig.LAYER_SIGUNGU_LINE, MapConfig.SOURCE_SIGUNGU_LINE).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineColor(levelColorStep { level -> RunvealDong.style(level).stroke.toArgb() }),
                PropertyFactory.lineWidth(LINE_WIDTH_DP),
            )
            layer.minZoom = MapConfig.SIDO_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            style.addLayer(layer)
        }
        if (style.getLayer(MapConfig.LAYER_SIGUNGU_LABEL) == null) {
            val layer = SymbolLayer(MapConfig.LAYER_SIGUNGU_LABEL, MapConfig.SOURCE_SIGUNGU_LABEL).withProperties(
                PropertyFactory.textField(Expression.get("name")),
                PropertyFactory.textFont(LABEL_FONT),
                PropertyFactory.textSize(LABEL_SIZE_SP),
                PropertyFactory.textColor(colors.sigunguLabel),
                PropertyFactory.textJustify(Property.TEXT_JUSTIFY_CENTER),
                // 250개 고정 목록(동 레벨처럼 수천 개가 아님)이라 겹침 방지로 숨기지 않고 항상 보이게 한다.
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true),
            )
            layer.minZoom = MapConfig.SIDO_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            style.addLayer(layer)
        }
    }
}
