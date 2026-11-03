package com.swyp.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.MapConfig

/** 지도의 초기 카메라(중심 좌표 + 줌). */
@Stable
class MapState(
    val initialCenter: LatLon,
    val initialZoom: Double,
)

@Composable
fun rememberMapState(
    initialCenter: LatLon = MapConfig.SEOUL_CITY_HALL,
    initialZoom: Double = MapConfig.INITIAL_ZOOM,
): MapState = remember(initialCenter, initialZoom) { MapState(initialCenter, initialZoom) }
