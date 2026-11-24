package com.sloth.runveal.tabhome

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
import com.sloth.runveal.core.map.DongOverlayHolder
import com.sloth.runveal.core.map.LocalMapState
import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.network.dong.CurrentDongHolder
import com.sloth.runveal.feature.tabhome.R
import com.sloth.runveal.tabhome.component.HomeMapOverlay
import com.sloth.runveal.tabhome.component.TOGGLE_INDEX_UNEXPLORED
import com.sloth.runveal.tabhome.component.ZoomedInMapOverlay

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
    // 필요한데, 홈 라벨은 그만한 정확도가 필요 없다. 홈 배너의 "다시 시도"를 누르면 그 횟수가
    // 바뀌면서 여기로 돌아온다(배너는 다른 모듈이라 이 ViewModel을 직접 못 부른다).
    val retryRequests by CurrentDongHolder.retryRequests.collectAsStateWithLifecycle()
    LaunchedEffect(retryRequests) { viewModel.loadCurrentDong(context) }
    val isZoomedIn = mapState.currentZoom >= MapConfig.ZOOM_IN_THRESHOLD
    var selectedToggleIndex by remember { mutableIntStateOf(0) }

    // 카메라가 멈출 때마다 지도 가운데 동을 다시 묻는다 — mapState.currentCenter는 멈출 때만
    // 바뀌므로(OnCameraIdle) 끌고 있는 동안에는 요청이 나가지 않는다. 줌인 뷰는 내가 서 있는 동을
    // 쓰니 줌아웃일 때만 부른다.
    LaunchedEffect(mapState.currentCenter, isZoomedIn) {
        if (!isZoomedIn) viewModel.loadCenterDong(mapState.currentCenter)
    }

    // "안 가본 길"을 고른 동안만 베이스 지도 도로를 강조한다 — 줌 아웃되거나 이 화면을 벗어나면 원래대로.
    val unexploredRoadFocus = isZoomedIn && selectedToggleIndex == TOGGLE_INDEX_UNEXPLORED
    DisposableEffect(unexploredRoadFocus) {
        mapState.unexploredRoadFocus = unexploredRoadFocus
        onDispose { mapState.unexploredRoadFocus = false }
    }

    // 시군구 없이 행정동만 — 지도 위 한 줄짜리 자리라 짧은 쪽이 읽기 좋다.
    // 아직 못 받았으면(위치 권한 없음·서비스 지역 밖·일시 오류) 대체 문구를 쓴다.
    //
    // 줌인 뷰는 **내가 서 있는 동**([HomeMapOverlayState.dongName])을, 줌아웃 뷰는 **지도 가운데
    // 보이는 동**([HomeMapOverlayState.centerDongName])을 쓴다 — 줌아웃에서는 지도를 끌어 다른
    // 동네를 둘러보는 게 목적이라 화면에 보이는 곳의 이름이 맞다.
    val locationName = state.dongName ?: stringResource(R.string.home_map_current_location)
    val centerLocationName = state.centerDongName ?: stringResource(R.string.home_map_current_location)

    // 탐험률·거리는 지도 빛을 정하는 값과 **같은 출처**를 쓴다 — 따로 세면 헤더엔 8%인데 지도는
    // 다른 단계로 빛나는 식으로 어긋난다. 오버레이가 아직 안 만들어졌거나 0%면 0이다.
    val overlayBundle by DongOverlayHolder.state.collectAsStateWithLifecycle()
    val dongCode = state.dongCode
    val ratePercent = dongCode?.let { overlayBundle?.dongPercents?.get(it) } ?: 0
    val exploredKm = dongCode?.let { overlayBundle?.walkedLengthsByDong?.get(it) }?.div(1000.0) ?: 0.0
    val totalKm = dongCode?.let { overlayBundle?.totalLengthsByDong?.get(it) }?.div(1000.0) ?: 0.0

    if (!isZoomedIn) {
        HomeMapOverlay(locationName = centerLocationName, modifier = modifier.fillMaxSize())
    } else {
        // TODO: 동 상세 진입(onDongClick) 추가 예정.
        ZoomedInMapOverlay(
            dongName = locationName,
            ratePercent = ratePercent,
            exploredKm = exploredKm,
            totalKm = totalKm,
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
