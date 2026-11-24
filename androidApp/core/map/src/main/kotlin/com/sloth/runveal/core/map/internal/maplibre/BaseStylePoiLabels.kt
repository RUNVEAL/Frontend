package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer

/**
 * 줌인(러닝 포커스)에서 **역·터미널 같은 큰 기준점 이름**을 띄운다.
 *
 * 베이스 스타일(`openfreemap/dark`)에는 POI 레이어 정의가 아예 없지만 **타일 소스에는 `poi` 레이어가
 * 들어 있다** — 그래서 쓰려면 레이어를 직접 꽂아야 한다([BaseStyleDarkCorrection]이 다크 스타일에
 * 없는 `landcover_grass`를 추가하는 것과 같은 방식).
 *
 * **줌인에서만 켠다.** 이 앱은 베이스 스타일의 지명·도로명 라벨을 전부 숨기는데
 * ([BaseStyleLabels]), 줌아웃에서는 동/시군구/시도 라벨이 주인공이라 거기에 장소 이름이 섞이면
 * 서로를 가린다. 줌인은 도로가 주인공이라 역 이름이 길을 찾는 데 도움이 된다.
 */
internal object BaseStylePoiLabels {

    /** 동 라벨(11)보다 작게 — 기준점이지 주인공이 아니다. */
    private const val LABEL_SIZE_SP = 10f

    /** 글자가 아이콘 자리(점) 아래에 붙게. 단위는 em이라 글자 크기에 비례한다. */
    private val LABEL_OFFSET_EM = arrayOf(0f, 0.8f)

    private const val LABEL_HALO_WIDTH_DP = 1.2f

    private val LABEL_FONT = arrayOf("Noto Sans Regular")

    fun show(style: Style, colors: MapColors) {
        if (style.getLayer(MapConfig.LAYER_BASE_POI_LABEL) != null) return

        val layer = SymbolLayer(MapConfig.LAYER_BASE_POI_LABEL, MapConfig.SOURCE_BASE_STYLE).withProperties(
            PropertyFactory.textField(Expression.get("name")),
            PropertyFactory.textFont(LABEL_FONT),
            PropertyFactory.textSize(LABEL_SIZE_SP),
            PropertyFactory.textColor(colors.poiLabel),
            PropertyFactory.textHaloColor(colors.poiLabelHalo),
            PropertyFactory.textHaloWidth(LABEL_HALO_WIDTH_DP),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOffset(LABEL_OFFSET_EM),
            // 겹치면 지운다(기본값) — 글자가 서로 포개지면 둘 다 못 읽는다.
            PropertyFactory.textAllowOverlap(false),
        )
        layer.setSourceLayer(MapConfig.SOURCE_LAYER_BASE_POI)
        layer.setFilter(subclassFilter())
        layer.minZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
        style.addLayer(layer)
    }

    /**
     * `subclass`가 [MapConfig.BASE_POI_LABEL_SUBCLASSES] 중 하나인 것만.
     *
     * `Expression.`in`()` 대신 eq를 or로 묶는다 — 백틱이 필요한 이름이라 읽기 나쁘고, 버전에 따라
     * 인자 형태가 달라 조용히 안 맞을 수 있다.
     */
    private fun subclassFilter(): Expression = Expression.any(
        *MapConfig.BASE_POI_LABEL_SUBCLASSES
            .map { Expression.eq(Expression.get("subclass"), Expression.literal(it)) }
            .toTypedArray(),
    )
}
