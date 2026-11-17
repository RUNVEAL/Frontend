package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory

/**
 * 동 경계가 주인공인 뷰([MapConfig.ZOOM_IN_THRESHOLD] 미만 — 시도/시군구/동 오버레이가 보이는 구간)에서는
 * 베이스 스타일(OpenFreeMap dark)의 도로/수역/녹지 등을 흐리게 깔아 우리 경계/라벨이 더 도드라지게
 * 한다. [MapConfig.ZOOM_IN_THRESHOLD] 이상(러닝 포커스)에서는 원래 밝기로 돌아와 도로가 또렷하게 보인다.
 *
 * 스타일이 막 로드된 직후의 레이어 스냅샷만 대상으로 하므로, 그 뒤에 우리가 추가하는 동/시군구/시도
 * 레이어는 자동으로 제외된다 — 그래서 다른 레이어를 추가하기 전에 가장 먼저 호출해야 한다.
 */
internal object BaseMapDimmer {
    private const val LINE_OPACITY_DIMMED = 0.4f
    private const val FILL_OPACITY_DIMMED = 0.3f

    /** 간선/고속도로는 일반 도로(0.4)보다 더 흐리게 — 두꺼운 선이라 0.4로도 과하게 두드러져서. */
    private const val MAJOR_ROAD_LINE_OPACITY_DIMMED = 0.15f

    fun applyAtStyleLoad(style: Style) {
        val fullBrightnessAtZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
        style.layers.forEach { layer ->
            when (layer) {
                is LineLayer -> layer.setProperties(PropertyFactory.lineOpacity(dimStep(fullBrightnessAtZoom, LINE_OPACITY_DIMMED)))
                is FillLayer -> layer.setProperties(PropertyFactory.fillOpacity(dimStep(fullBrightnessAtZoom, FILL_OPACITY_DIMMED)))
                else -> Unit
            }
        }
        // 일반 도로 딤 처리 위에 간선/고속도로만 한 번 더 낮춘 값으로 덮어쓴다.
        MapConfig.BASE_STYLE_MAJOR_ROAD_LAYERS.forEach { layerId ->
            style.getLayerAs<LineLayer>(layerId)?.setProperties(
                PropertyFactory.lineOpacity(dimStep(fullBrightnessAtZoom, MAJOR_ROAD_LINE_OPACITY_DIMMED)),
            )
        }
    }

    /** `zoom` 미만은 [dimmedOpacity]로 낮추고, 그 이상(러닝 포커스)은 원래 불투명도(1.0)로 돌아온다. */
    private fun dimStep(zoom: Float, dimmedOpacity: Float): Expression =
        Expression.step(Expression.zoom(), Expression.literal(dimmedOpacity), Expression.stop(zoom, Expression.literal(1f)))
}
