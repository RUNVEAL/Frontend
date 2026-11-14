package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue

/**
 * 홈 줌인 뷰에서 "안 가본 길"을 고른 동안, 베이스 스타일(OpenFreeMap dark)의 도로
 * ([MapConfig.BASE_STYLE_ROAD_LAYERS])를 디자인 토큰 `map.roadUnexploredFocus`(neutral/400) 한 색으로
 * 덮어 강조한다. 다크 스타일의 도로는 원래 거의 검정(`highway_major_inner` = `hsl(0,0%,7%)`)이고 그
 * 아래 깔린 casing만 살짝 밝아서, 가본 길(금색 선)이 어디까지 뻗어 있는지는 보여도 "아직 안 간 길"이
 * 어디로 이어지는지는 알아보기 어렵다. 가본 길 레이어([VisitedRoadLayer])는 건드리지 않는다.
 *
 * 토글을 끄면 원래 색으로 되돌려야 하므로, 스타일이 로드된 직후의 `line-color`를 한 번 기억해 둔다
 * ([rememberOriginalColors]) — 반드시 [apply]보다 먼저 호출해야 한다.
 */
internal class BaseStyleRoadFocus {
    private val originalColors = mutableMapOf<String, Expression>()

    /** 스타일 로드 직후, 아직 아무도 도로 색을 건드리지 않은 상태에서 한 번 호출한다. */
    fun rememberOriginalColors(style: Style) {
        originalColors.clear()
        MapConfig.BASE_STYLE_ROAD_LAYERS.forEach { layerId ->
            val layer = style.getLayerAs<LineLayer>(layerId) ?: return@forEach
            originalColors[layerId] = layer.lineColor.toColorExpression() ?: return@forEach
        }
    }

    /** [focusColor]가 null이면 [rememberOriginalColors]로 기억해 둔 원래 색으로 되돌린다. */
    fun apply(style: Style, focusColor: Int?) {
        val focusExpression = focusColor?.let { Expression.color(it) }
        MapConfig.BASE_STYLE_ROAD_LAYERS.forEach { layerId ->
            val layer = style.getLayerAs<LineLayer>(layerId) ?: return@forEach
            val color = focusExpression ?: originalColors[layerId] ?: return@forEach
            layer.setProperties(PropertyFactory.lineColor(color))
        }
    }

    /**
     * `line-color`는 단색(`rgba(…)` 문자열)일 수도, 줌에 따라 색이 바뀌는 식일 수도 있다
     * (`highway_motorway_inner`가 후자) — 둘 다 그대로 다시 설정할 수 있는 식 하나로 맞춰 둔다.
     */
    private fun PropertyValue<String>.toColorExpression(): Expression? =
        if (isExpression) expression else getColorInt()?.let { Expression.color(it) }
}
