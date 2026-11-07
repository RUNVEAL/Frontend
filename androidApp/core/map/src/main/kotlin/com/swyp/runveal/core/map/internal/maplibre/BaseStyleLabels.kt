package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.map.MapConfig
import org.maplibre.android.maps.Style

/**
 * 기본 스타일(OpenFreeMap dark)에 내장된 지명/도로 이름 라벨은 우리 동/시군구 라벨이 나타나는
 * 시점([MapConfig.SIGUNGU_SWITCH_ZOOM])부터 겹쳐 보이므로, 그 지점 이상에서는 숨긴다. 스타일이
 * 로드된 직후 한 번만 호출하면 된다 — 레이어 자체의 maxZoom을 낮추는 것뿐이라 줌 리스너가 따로 없다.
 */
internal object BaseStyleLabels {
    fun hideFromDongZoom(style: Style) {
        val maxZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
        (MapConfig.BASE_STYLE_PLACE_LABEL_LAYERS + MapConfig.BASE_STYLE_ROAD_LABEL_LAYERS).forEach { layerId ->
            style.getLayer(layerId)?.maxZoom = maxZoom
        }
    }
}
