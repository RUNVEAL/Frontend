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

    /** 메인 동 라벨 배율 — body/m-semibold(14) · num/l(32) · unit/l(20)을 기준 11로 나눈 값. */
    private const val MAIN_NAME_FONT_SCALE = 14f / LABEL_SIZE_SP
    private const val MAIN_PERCENT_VALUE_FONT_SCALE = 32f / LABEL_SIZE_SP
    private const val MAIN_PERCENT_UNIT_FONT_SCALE = 20f / LABEL_SIZE_SP

    /** 지도 글리프에 SemiBold가 없어 Bold로 근사한다(Regular/Bold만 제공). */
    private val LABEL_FONT_BOLD = arrayOf("Noto Sans Bold")

    /** 일반 라벨의 sortKey는 `-탐험률`이라 최솟값이 -100이다 — 그보다 작게 둬서 항상 먼저 놓인다. */
    private const val MAIN_LABEL_SORT_KEY = -1000f

    private val EMPTY_FEATURE_COLLECTION_JSON = FeatureCollection.fromFeatures(emptyList()).toJson()

    fun apply(style: Style, overlay: DongOverlayGeoJson?, colors: MapColors, mainDongCode: String?) {
        ensureSource(style, MapConfig.SOURCE_DONG_FILL, overlay?.fillGeoJson)
        ensureSource(style, MapConfig.SOURCE_DONG_LINE, overlay?.lineGeoJson)
        ensureSource(style, MapConfig.SOURCE_DONG_LABEL, overlay?.labelGeoJson)
        ensureLayers(style, colors)
        applyMainDong(style, mainDongCode)
    }

    /**
     * 메인 동이 바뀔 때마다 두 라벨 레이어의 필터만 갈아 끼운다 — 일반 레이어는 그 동을 빼고,
     * 메인 레이어는 그 동만 그린다. 같은 동이 두 크기로 겹쳐 보이지 않게 **서로 배타적**이어야 한다.
     *
     * 메인이 없으면(null) 일반 레이어가 전부 그리고 메인 레이어는 아무것도 그리지 않는다.
     */
    private fun applyMainDong(style: Style, mainDongCode: String?) {
        val isMain = mainDongCode?.let { Expression.eq(Expression.get("adm_cd2"), Expression.literal(it)) }
        style.getLayerAs<SymbolLayer>(MapConfig.LAYER_DONG_LABEL)
            ?.setFilter(isMain?.let { Expression.not(it) } ?: Expression.literal(true))
        style.getLayerAs<SymbolLayer>(MapConfig.LAYER_DONG_LABEL_MAIN)
            ?.setFilter(isMain ?: Expression.literal(false))
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
        if (style.getLayer(MapConfig.LAYER_DONG_LABEL_MAIN) == null) {
            val layer = SymbolLayer(MapConfig.LAYER_DONG_LABEL_MAIN, MapConfig.SOURCE_DONG_LABEL).withProperties(
                PropertyFactory.textField(mainLabelTextField(colors)),
                // body/m-semibold의 600을 지도에서 낼 수 없다 — 글리프에 Regular와 Bold만 있어서
                // (SemiBold는 404) 더 굵은 쪽으로 근사한다.
                PropertyFactory.textFont(LABEL_FONT_BOLD),
                PropertyFactory.textSize(LABEL_SIZE_SP),
                PropertyFactory.textColor(colors.dongLabelPrimary),
                PropertyFactory.textJustify(Property.TEXT_JUSTIFY_CENTER),
                // 메인 동 라벨은 겹쳐도 지지 않아야 한다 — 가장 먼저 자리를 잡게 sortKey를 최하로 둔다.
                PropertyFactory.symbolSortKey(Expression.literal(MAIN_LABEL_SORT_KEY)),
                // 커진 라벨이 이웃 라벨에 밀려 사라지지 않게.
                PropertyFactory.symbolZOrder(Property.SYMBOL_Z_ORDER_SOURCE),
            )
            layer.minZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
            style.addLayerAbove(layer, MapConfig.LAYER_DONG_LABEL)
        }
    }

    /**
     * 동 이름(caption/s-regular) 줄과 "12%"(num/s + unit/s) 줄. 토큰대로 숫자만 크고(14),
     * 이름과 % 기호는 작고 연한 색이다.
     */
    /**
     * 메인 동 라벨 — 이름 body/m-semibold(14), 숫자 num/l(32), % unit/l(20).
     * 기준 [LABEL_SIZE_SP](11)에 각 토큰 크기를 나눈 배율을 세그먼트마다 준다.
     */
    private fun mainLabelTextField(colors: MapColors): Expression = Expression.format(
        Expression.formatEntry(
            Expression.get("name"),
            Expression.FormatOption.formatFontScale(MAIN_NAME_FONT_SCALE.toDouble()),
            Expression.FormatOption.formatTextColor(colors.dongLabelPrimary),
        ),
        Expression.formatEntry(Expression.literal("\n")),
        Expression.formatEntry(
            Expression.get("percentValue"),
            Expression.FormatOption.formatFontScale(MAIN_PERCENT_VALUE_FONT_SCALE.toDouble()),
            Expression.FormatOption.formatTextColor(colors.dongLabelPrimary),
        ),
        Expression.formatEntry(
            Expression.literal("%"),
            Expression.FormatOption.formatFontScale(MAIN_PERCENT_UNIT_FONT_SCALE.toDouble()),
            Expression.FormatOption.formatTextColor(colors.dongLabelSecondary),
        ),
    )

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
