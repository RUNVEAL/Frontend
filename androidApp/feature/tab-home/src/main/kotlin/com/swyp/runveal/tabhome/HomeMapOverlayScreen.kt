package com.swyp.runveal.tabhome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.map.MapConfig
import com.swyp.runveal.tabhome.component.HomeMapOverlay
import com.swyp.runveal.tabhome.component.TOGGLE_INDEX_UNEXPLORED
import com.swyp.runveal.tabhome.component.ZoomedInMapOverlay

/**
 * 홈 지도 오버레이. 지도 자체(`RunvealMap`)와 위치 권한 요청은 Map Shell(`feature:home`)이 맡고,
 * 여기서는 줌 레벨을 [LocalMapState]에서 읽어 그 위에 얹는 UI만 그린다.
 */
@Composable
internal fun HomeMapOverlayScreen(
    modifier: Modifier = Modifier,
    onCreateCourseClick: () -> Unit = {},
    onRunStartClick: () -> Unit = {},
) {
    val mapState = LocalMapState.current
    val isZoomedIn = mapState.currentZoom >= MapConfig.ZOOM_IN_THRESHOLD
    var selectedToggleIndex by remember { mutableIntStateOf(0) }

    // "안 가본 길"을 고른 동안만 베이스 지도 도로를 강조한다 — 줌 아웃되거나 이 화면을 벗어나면 원래대로.
    val unexploredRoadFocus = isZoomedIn && selectedToggleIndex == TOGGLE_INDEX_UNEXPLORED
    DisposableEffect(unexploredRoadFocus) {
        mapState.unexploredRoadFocus = unexploredRoadFocus
        onDispose { mapState.unexploredRoadFocus = false }
    }

    if (!isZoomedIn) {
        HomeMapOverlay(locationName = "현재 위치", modifier = modifier.fillMaxSize())
    } else {
        // TODO: 동 상세 진입, 실제 탐험률/거리 연동 추가 예정
        ZoomedInMapOverlay(
            dongName = "성수동",
            ratePercent = 0,
            exploredKm = 0.0,
            totalKm = 0.0,
            selectedToggleIndex = selectedToggleIndex,
            onToggleSelect = { selectedToggleIndex = it },
            onDongClick = {},
            onCreateCourseClick = onCreateCourseClick,
            onRunStartClick = onRunStartClick,
            onMyLocationClick = mapState::recenterToCurrentLocation,
            modifier = modifier.fillMaxSize(),
        )
    }
}
