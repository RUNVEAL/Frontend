package com.swyp.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.MapConfig

/** 지도의 초기 카메라(중심 좌표 + 줌), 현재 줌, 하위 화면이 선언한 레이어([MapLayers])를 들고 있다. */
@Stable
class MapState(
    val initialCenter: LatLon,
    val initialZoom: Double,
) {
    var currentZoom: Double by mutableDoubleStateOf(initialZoom)
        internal set

    private val _layers = mutableStateMapOf<String, String>()
    internal val layers: Map<String, String> get() = _layers

    internal fun setLayer(id: String, geoJson: String) {
        _layers[id] = geoJson
    }

    internal fun clearLayer(id: String) {
        _layers.remove(id)
    }
}

@Composable
fun rememberMapState(
    initialCenter: LatLon = MapConfig.SEOUL_CITY_HALL,
    initialZoom: Double = MapConfig.INITIAL_ZOOM,
): MapState = remember(initialCenter, initialZoom) { MapState(initialCenter, initialZoom) }

/** Map Shell(지도를 그리는 화면) 안에서만 제공된다. 지도 상태를 읽거나 [MapLayers]로 레이어를 선언할 때 쓴다. */
val LocalMapState = staticCompositionLocalOf<MapState> {
    error("MapState가 제공되지 않았다. Map Shell 안에서만 사용할 수 있다.")
}
