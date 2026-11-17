package com.swyp.runveal.tabhome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.map.MapConfig
import com.swyp.runveal.feature.tabhome.R
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
    viewModel: HomeMapOverlayViewModel = viewModel(),
) {
    val mapState = LocalMapState.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 화면에 들어올 때 한 번만 조회한다 — 걸어 다니며 동이 바뀌는 것까지 따라가려면 위치 구독이
    // 필요한데, 홈 라벨은 그만한 정확도가 필요 없다.
    LaunchedEffect(Unit) { viewModel.loadCurrentDong(context) }
    val isZoomedIn = mapState.currentZoom >= MapConfig.ZOOM_IN_THRESHOLD
    var selectedToggleIndex by remember { mutableIntStateOf(0) }

    // "안 가본 길"을 고른 동안만 베이스 지도 도로를 강조한다 — 줌 아웃되거나 이 화면을 벗어나면 원래대로.
    val unexploredRoadFocus = isZoomedIn && selectedToggleIndex == TOGGLE_INDEX_UNEXPLORED
    DisposableEffect(unexploredRoadFocus) {
        mapState.unexploredRoadFocus = unexploredRoadFocus
        onDispose { mapState.unexploredRoadFocus = false }
    }

    // 아직 못 받았으면(위치 권한 없음·서비스 지역 밖·일시 오류) 대체 문구를 쓴다.
    val locationName = state.dongName ?: stringResource(R.string.home_map_current_location)

    if (!isZoomedIn) {
        HomeMapOverlay(locationName = locationName, modifier = modifier.fillMaxSize())
    } else {
        // TODO: 동 상세 진입, 실제 탐험률/거리 연동 추가 예정 — 분모·분자는 state.dongCode로 찾는다.
        ZoomedInMapOverlay(
            dongName = locationName,
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
