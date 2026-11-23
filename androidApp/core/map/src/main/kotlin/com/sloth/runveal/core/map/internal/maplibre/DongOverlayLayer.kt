package com.sloth.runveal.core.map.internal.maplibre

import androidx.compose.ui.graphics.toArgb
import com.sloth.runveal.core.map.DongOverlayGeoJson
import com.sloth.runveal.designsystem.map.RunvealDong
import com.sloth.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection

/**
 * 동 채우기/테두리/라벨. 면은 완주율 단계와 무관하게 [RunvealDong.style] 0단계(투명) 고정이다 — 단계별
 * 밝기는 [com.sloth.runveal.core.map.internal.maplibre.DongGlowImageLayer]가 올리는 빛 이미지가 전담하므로,
 * 그 아래 면까지 단계별로 다르면 이미지 로딩 전후로 같은 동이 두 번 다른 색으로 보인다. 테두리는 반대로
 * [RunvealDong.style]의 단계별 색을 그대로 써서, 빛 이미지가 뜨기 전에도 테두리만으로 완주율 단계를
 * 바로 알 수 있게 한다. 라벨 글자색만 Composable 컨텍스트가 필요해서 [MapColors]로 받는다. 탭해서
 * 강조하는 선택 상태 같은 인터랙션은 feature 레이어 몫이라 이번 범위엔 넣지 않는다.
 *
 * [MapConfig.SIGUNGU_SWITCH_ZOOM]~[MapConfig.ZOOM_IN_THRESHOLD] 사이에서만 보인다. 그보다 축소하면
 * [SigunguOverlayLayer]의 시군구 레이어로 전환되고, 그보다 확대해 홈 탭이 "줌인"(러닝 시작 UI)
 * 상태로 바뀌면 동 라벨은 숨는다 — 러닝 포커스 화면에서 동 퍼센티지가 거슬리지 않게 하기 위함.
 */
internal object DongOverlayLayer {
    private const val LINE_WIDTH_DP = 1.2f
    /**
     * 라벨 글자 크기의 기준 — 동 이름(caption/s-regular)과 % 기호(unit/s)가 둘 다 11이라 이걸
     * 기준으로 두고, 더 큰 숫자(num/s = 14)만 [PERCENT_VALUE_FONT_SCALE]로 키운다.
     *
     * 글꼴은 지도 스타일이 가진 glyph(Noto Sans)만 쓸 수 있어서 Pretendard/Geist로는 못 바꾼다 —
     * 토큰에서 **크기와 색만** 옮겨 온다.
     */
    private const val LABEL_SIZE_SP = 11f

    /** num/s(14) ÷ 기준 11 — 퍼센트 숫자만 이만큼 키운다. */
    private const val PERCENT_VALUE_FONT_SCALE = 14f / LABEL_SIZE_SP
    private val LABEL_FONT = arrayOf("Noto Sans Regular")

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, overlay: DongOverlayGeoJson?, colors: MapColors) {
        ensureSource(style, MapConfig.SOURCE_DONG_FILL, overlay?.fillGeoJson)
        ensureSource(style, MapConfig.SOURCE_DONG_LINE, overlay?.lineGeoJson)
        ensureSource(style, MapConfig.SOURCE_DONG_LABEL, overlay?.labelGeoJson)
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
        val baseStyle = RunvealDong.style(0)
        if (style.getLayer(MapConfig.LAYER_DONG_FILL) == null) {
            val layer = FillLayer(MapConfig.LAYER_DONG_FILL, MapConfig.SOURCE_DONG_FILL).withProperties(
                PropertyFactory.fillColor(baseStyle.fill.toArgb()),
            )
            layer.minZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
            style.addLayer(layer)
        }
        if (style.getLayer(MapConfig.LAYER_DONG_LINE) == null) {
            val layer = LineLayer(MapConfig.LAYER_DONG_LINE, MapConfig.SOURCE_DONG_LINE).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineColor(levelColorStep { level -> RunvealDong.style(level).stroke.toArgb() }),
                PropertyFactory.lineWidth(LINE_WIDTH_DP),
            )
            layer.minZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
            style.addLayer(layer)
        }
        if (style.getLayer(MapConfig.LAYER_DONG_LABEL) == null) {
            val layer = SymbolLayer(MapConfig.LAYER_DONG_LABEL, MapConfig.SOURCE_DONG_LABEL).withProperties(
                PropertyFactory.textField(labelTextField(colors)),
                PropertyFactory.textFont(LABEL_FONT),
                PropertyFactory.textSize(LABEL_SIZE_SP),
                PropertyFactory.textColor(colors.dongLabelPrimary),
                PropertyFactory.textJustify(Property.TEXT_JUSTIFY_CENTER),
                // 겹쳐서 하나만 남을 때 탐험률 높은 동을 살린다 — sortKey가 낮은 라벨이 먼저 배치돼
                // 자리를 차지하므로 탐험률에 -1을 곱한다([SigunguOverlayLayer]와 같은 규칙).
                PropertyFactory.symbolSortKey(
                    Expression.product(Expression.literal(-1f), Expression.get("percent")),
                ),
            )
            layer.minZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
            style.addLayer(layer)
        }
    }

    /**
     * 동 이름(caption/s-regular) 줄과 "12%"(num/s + unit/s) 줄. 토큰대로 숫자만 크고(14),
     * 이름과 % 기호는 작고 연한 색이다.
     */
    private fun labelTextField(colors: MapColors): Expression = Expression.format(
        Expression.formatEntry(Expression.get("name"), Expression.FormatOption.formatTextColor(colors.dongLabelSecondary)),
        Expression.formatEntry(Expression.literal("\n")),
        Expression.formatEntry(
            Expression.get("percentValue"),
            Expression.FormatOption.formatFontScale(PERCENT_VALUE_FONT_SCALE.toDouble()),
            Expression.FormatOption.formatTextColor(colors.dongLabelPrimary),
        ),
        Expression.formatEntry(
            Expression.literal("%"),
            Expression.FormatOption.formatTextColor(colors.dongLabelSecondary),
        ),
    )
}
