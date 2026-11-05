package com.swyp.runveal.tabhome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.swyp.runveal.core.map.RunvealMap
import com.swyp.runveal.map.MapConfig
import com.swyp.runveal.permission.RequireLocationPermission
import com.swyp.runveal.tabhome.component.HomeMapOverlay
import com.swyp.runveal.tabhome.component.ZoomedInMapOverlay

/** A3-4(줌 인) 전환 기준. 전환 히스테리시스(16.0 / 15.7)는 추후 정교화. */
private const val ZOOM_IN_THRESHOLD = 16.0

@Composable
fun HomeMapScreen(modifier: Modifier = Modifier, onZoomedInChanged: (Boolean) -> Unit = {}) {
    var zoom by remember { mutableDoubleStateOf(MapConfig.INITIAL_ZOOM) }
    val isZoomedIn = zoom >= ZOOM_IN_THRESHOLD
    var selectedToggleIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isZoomedIn) { onZoomedInChanged(isZoomedIn) }

    RequireLocationPermission(modifier = modifier) {
        Box(modifier = modifier.fillMaxSize()) {
            RunvealMap(modifier = Modifier.fillMaxSize(), onZoomChanged = { zoom = it })
            if (!isZoomedIn) {
                HomeMapOverlay(locationName = "현재 위치", modifier = Modifier.fillMaxSize())
            } else {
                // TODO: 동 상세 진입, 코스 만들기, 러닝 시작, 내 위치 이동, 실제 탐험률/거리 연동 추가 예정
                ZoomedInMapOverlay(
                    dongName = "성수동",
                    ratePercent = 0,
                    exploredKm = 0.0,
                    totalKm = 0.0,
                    selectedToggleIndex = selectedToggleIndex,
                    onToggleSelect = { selectedToggleIndex = it },
                    onDongClick = {},
                    onCreateCourseClick = {},
                    onRunStartClick = {},
                    onMyLocationClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
