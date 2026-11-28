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
import com.sloth.runveal.core.map.MapOverlayLevel
import com.sloth.runveal.dong.DongExploreRate
import com.sloth.runveal.network.dong.CurrentDongHolder
import com.sloth.runveal.feature.tabhome.R
import com.sloth.runveal.tabhome.component.TOGGLE_INDEX_DONG
import com.sloth.runveal.tabhome.component.TOGGLE_INDEX_ROAD
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
    // 패널은 줌과 무관하게 한 가지(줌인 모양)다 — 줌으로 갈리던 분기를 없앴다.
    //
    // 기본값은 동네 단위 — 앱을 열면 탐험률이 가장 높은 동으로 포커스하는 기존 흐름이 동 단위
    // 보기라, 길 단위로 시작하면 첫 화면이 달라진다.
    var selectedToggleIndex by remember { mutableIntStateOf(TOGGLE_INDEX_DONG) }

    // 카메라가 멈출 때마다 지도 가운데 동을 다시 묻는다 — mapState.currentCenter는 멈출 때만
    // 바뀌므로(OnCameraIdle) 끌고 있는 동안에는 요청이 나가지 않는다.
    LaunchedEffect(mapState.currentCenter) {
        viewModel.loadCenterDong(mapState.currentCenter)
    }

    // 지도 라벨도 같은 동을 "메인"으로 크게 그린다 — 패널 이름과 지도에서 도드라지는 동이 달라지면
    // 어디를 보고 있는지 헷갈린다. 길 단위로 보는 동안은 동 라벨 자체가 안 보여서 쓰이지 않는다.
    DisposableEffect(state.centerDongCode) {
        mapState.mainDongCode = state.centerDongCode
        onDispose { mapState.mainDongCode = null }
    }

    // 토글이 지도 보기 단위를 정한다 — 줌은 더 이상 관여하지 않는다. 이 화면을 벗어나면 null로
    // 되돌려서 다른 지도 화면(러닝·코스 추천)이 예전처럼 줌으로 판단하게 둔다.
    //
    // 예전 "안 가본 길" 강조(mapState.unexploredRoadFocus)는 토글이 교체되면서 켤 자리가 없어졌다 —
    // MapState의 플래그와 그걸 쓰는 BaseStyleRoadFocus는 **남겨 두고 연결만 끊는다**(다른 자리에서
    // 되살릴 수 있다).
    val overlayLevel = if (selectedToggleIndex == TOGGLE_INDEX_ROAD) MapOverlayLevel.Road else MapOverlayLevel.Dong
    DisposableEffect(overlayLevel) {
        mapState.overlayLevel = overlayLevel
        onDispose { mapState.overlayLevel = null }
    }

    // 시군구 없이 행정동만 — 지도 위 한 줄짜리 자리라 짧은 쪽이 읽기 좋다.
    // 아직 못 받았으면(위치 권한 없음·서비스 지역 밖·일시 오류) 대체 문구를 쓴다.
    //
    // 보기 단위와 무관하게 **지도 가운데 보이는 동**([HomeMapOverlayState.centerDongName])을 쓴다 —
    // 지도를 끌어 다른 동네를 둘러보는 게 목적이라, 어느 줌에서든 화면에 보이는 곳의 이름이 맞다.
    // 내가 서 있는 동은 이제 꼬리표가 "현위치"인지 가리는 데만 쓴다([HomeMapOverlayState.dongCode]).
    val centerLocationName = state.centerDongName ?: stringResource(R.string.home_map_current_location)

    // 탐험률·거리는 지도 빛을 정하는 값과 **같은 출처**를 쓴다 — 따로 세면 헤더엔 8%인데 지도는
    // 다른 단계로 빛나는 식으로 어긋난다. 오버레이가 아직 안 만들어졌거나 0%면 0이다.
    val overlayBundle by DongOverlayHolder.state.collectAsStateWithLifecycle()
    // 꼬리표 기준 — 홈 요약 카드와 같은 함수를 써서 두 패널이 같은 동을 "내 동네"로 본다.
    val topExploredDongCode = overlayBundle?.dongPercents?.let(DongExploreRate::topDongCode)
    // 이름과 같은 동을 가리켜야 한다 — 이름은 지도 가운데 동인데 퍼센트가 내가 선 동이면 둘이 어긋난다.
    val dongCode = state.centerDongCode
    val ratePercent = dongCode?.let { overlayBundle?.dongPercents?.get(it) } ?: 0
    val exploredKm = dongCode?.let { overlayBundle?.walkedLengthsByDong?.get(it) }?.div(1000.0) ?: 0.0
    val totalKm = dongCode?.let { overlayBundle?.totalLengthsByDong?.get(it) }?.div(1000.0) ?: 0.0

    // 줌아웃 전용 패널(HomeMapOverlay)은 더 쓰지 않는다 — 패널을 줌인 모양 하나로 통일했다.
    // 그 컴포저블은 지우지 않고 남겨 뒀다(호출만 끊었다).
    //
    // TODO: 동 상세 진입(onDongClick) 추가 예정.
    ZoomedInMapOverlay(
        dongName = centerLocationName,
        // 줌아웃 요약 카드와 같은 기준 — 지도 가운데 동이 탐험률 최고면 "내 동네", 지금 서 있는
        // 동이면 "현위치", 그 밖(끌어서 둘러보는 동네)이면 안 붙인다.
        tagLabel = when (state.centerDongCode) {
            null -> null
            topExploredDongCode -> stringResource(R.string.home_zoomed_in_tag_my_dong)
            state.dongCode -> stringResource(R.string.home_zoomed_in_tag_current_location)
            else -> null
        },
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
