package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory

/**
 * 기본 스타일(OpenFreeMap dark)에 내장된 지명/도로 이름 라벨과 행정구역 경계선은 동/시군구/시도
 * 어느 줌에서든 우리 자체 라벨·테두리와 겹쳐 보이므로 모든 줌에서 항상 숨긴다. 스타일이 로드된
 * 직후 한 번만 호출하면 된다.
 */
internal object BaseStyleLabels {
    fun hide(style: Style) {
        (
            MapConfig.BASE_STYLE_PLACE_LABEL_LAYERS +
                MapConfig.BASE_STYLE_ROAD_LABEL_LAYERS +
                MapConfig.BASE_STYLE_BOUNDARY_LAYERS
            ).forEach { layerId ->
            style.getLayer(layerId)?.setProperties(PropertyFactory.visibility(Property.NONE))
        }
    }
}
