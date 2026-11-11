package com.swyp.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.component.map.RunvealMapMarkerType
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.MapConfig

/** 지도 위에 떠 있는 핀 마커 하나(출발/도착 선택 등) — 동시에 하나만 표시한다. */
data class MapPin(val location: LatLon, val type: RunvealMapMarkerType)

/** 지도의 초기 카메라(중심 좌표 + 줌), 현재 줌, 하위 화면이 선언한 레이어([MapLayers])를 들고 있다. */
@Stable
class MapState(
    val initialCenter: LatLon,
    val initialZoom: Double,
) {
    var currentZoom: Double by mutableDoubleStateOf(initialZoom)
        internal set

    /** 지도 위에 그릴 핀. 하위 화면(예: 출발/도착 선택)이 쓰고 지운다. */
    var pin: MapPin? by mutableStateOf(null)

    /** 지도를 탭한 마지막 좌표 — [MapLibreMapView]가 탭마다 채우고, 소비한 화면이 바로 null로 되돌려
     * 한 번만 처리되는 이벤트처럼 쓴다. */
    var lastMapClick: LatLon? by mutableStateOf(null)

    /** 0부터 시작 — [MapLibreMapView]가 이 값이 바뀔 때마다(첫 조립 제외) 현재 위치로 카메라를 다시
     * 옮긴다. 값 자체는 의미가 없고 변경 이벤트만 쓴다(일종의 one-shot 커맨드). */
    var recenterRequestId: Int by mutableIntStateOf(0)
        internal set

    private val _layers = mutableStateMapOf<String, String>()
    internal val layers: Map<String, String> get() = _layers

    internal fun setLayer(id: String, geoJson: String) {
        _layers[id] = geoJson
    }

    internal fun clearLayer(id: String) {
        _layers.remove(id)
    }

    /** "내 위치" 버튼 등에서 호출 — 위치 권한/서비스가 꺼져 있으면 [CurrentLocationLayer]가 조용히
     * 무시한다. */
    fun recenterToCurrentLocation() {
        recenterRequestId++
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

/**
 * Map Shell이 지도 위에 떠 있는 하단 패널(탭바 등)에 얹은 높이 — 줌인 오버레이의 하단 액션 줄처럼
 * NavHost 안 화면이 그 패널과 겹치지 않게 띄워야 할 때 쓴다. Map Shell 밖에서는 항상 0.dp.
 */
val LocalMapOverlayBottomInset = compositionLocalOf { 0.dp }
