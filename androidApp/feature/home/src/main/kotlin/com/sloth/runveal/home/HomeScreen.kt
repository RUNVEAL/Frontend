package com.sloth.runveal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import com.sloth.runveal.core.map.CourseSnapshotImage
import com.sloth.runveal.core.map.DongAssetRepository
import com.sloth.runveal.core.map.DongOverlayHolder
import com.sloth.runveal.core.map.LiveRoadClassifier
import com.sloth.runveal.core.map.LocalMapOverlayBottomInset
import com.sloth.runveal.core.map.LocalMapState
import com.sloth.runveal.core.map.MapLayerSpec
import com.sloth.runveal.core.map.MapLayers
import com.sloth.runveal.core.map.MapState
import com.sloth.runveal.core.map.RunvealMap
import com.sloth.runveal.core.map.buildVisitedRoadGeoJson
import com.sloth.runveal.core.map.loadDongOverlayBundle
import com.sloth.runveal.core.map.refreshDongOverlayFromServer
import com.sloth.runveal.core.map.rememberMapState
import com.sloth.runveal.courserecommend.navigation.courseDetailOverlay
import com.sloth.runveal.courserecommend.navigation.courseRecommendOverlay
import com.sloth.runveal.courserecommend.navigation.courseRecommendResultOverlay
import com.sloth.runveal.courserecommend.navigation.savedCourseDetailOverlay
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.sloth.runveal.designsystem.component.card.RunvealLocationOffBanner
import com.sloth.runveal.designsystem.component.card.RunvealSummaryCard
import com.sloth.runveal.designsystem.component.navigation.RunvealTabBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.sloth.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.sloth.runveal.designsystem.component.overlay.RunvealLocationPermissionSettingsSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealLocationPermissionSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealLocationServicesSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealToast
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.devicestate.rememberLocationServicesState
import com.sloth.runveal.dong.DongExploreRate
import com.sloth.runveal.feature.home.R
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.map.snapshot.DongThumbnailGeometry
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.center
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.navigation.rememberRunVealNavigator
import com.sloth.runveal.network.dong.CurrentDongHolder
import com.sloth.runveal.permission.LocationPermissionState
import com.sloth.runveal.permission.RequireLocationPermission
import com.sloth.runveal.permission.openAppSettings
import com.sloth.runveal.permission.rememberLocationPermissionState
import com.sloth.runveal.recording.RecordingState
import com.sloth.runveal.recording.TrackRecorder
import com.sloth.runveal.result.LiveMatchHolder
import com.sloth.runveal.result.LiveNewRoadHolder
import com.sloth.runveal.result.TrackResultHolder
import com.sloth.runveal.running.navigation.recordingOverlay
import com.sloth.runveal.running.navigation.resultOverlay
import com.sloth.runveal.tabcourse.navigation.courseGraph
import com.sloth.runveal.tabhome.navigation.homeMapOverlay
import com.sloth.runveal.tabmypage.navigation.myGraph
import com.sloth.runveal.tabrecord.navigation.recordGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HOME_BOTTOM_PANEL_INNER_INSET = 6.dp

/** 토스트가 떠 있는 시간 — 디자인시스템 토스트 주석의 "3초"를 그대로 따른다. */
private const val TOAST_DURATION_MS = 3000L

/**
 * 줌인 오버레이의 하단 액션 줄이 하단 패널 배너(위치 안내·서버 데이터 안내) 때문에 더 높아진 패널과
 * 안 겹치게 추가로 띄워야 하는 높이. 배너(40dp) + 패널 안쪽 top padding(12dp) + 배너·탭바 사이 gap(4dp) - 패널이 대신하는
 * 탭바 단독 바깥 padding(16dp) = 56dp. 매칭되는 토큰이 없어 직접 계산해 하드코딩했다.
 */
private val LOCATION_BANNER_EXTRA_INSET = 56.dp

/** 홈 하단 패널의 좌우 여백 — 화면 여백(16)의 예외. 위 주석 참고. */
private val HOME_PANEL_SIDE_MARGIN = 10.dp

/**
 * 홈 탭 화면 — Map Shell. 지도(`RunvealMap`)는 여기서 하나만 그리고, 탭 화면과 지도 위 흐름(홈 지도,
 * 코스 추천)은 전부 이 아래 단일 NavHost로 조립한다. 탭바 노출 규칙도 여기 한 곳에만 둔다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(onLoggedOut: () -> Unit, modifier: Modifier = Modifier) {
    val tabNavigator = rememberRunVealNavigator()
    val mapState = rememberMapState()
    val locationPermissionState = rememberLocationPermissionState()
    // 권한(앱)과 위치 서비스(기기)는 서로 다른 상태라 따로 본다 — 둘 중 하나만 꺼져 있어도 위치를 못 받는다.
    val locationServicesState = rememberLocationServicesState()
    val isLocationBannerNeeded = !locationPermissionState.isGranted || !locationServicesState.isEnabled
    val context = LocalContext.current

    /**
     * **러닝을 시작하기 전에** 위치가 준비됐는지 보고, 안 됐으면 그 자리에서 안내 바텀시트를 띄운다.
     *
     * 그냥 보내면 앱이 죽는다 — 러닝 기록은 위치 권한이 있어야 시작할 수 있는 Foreground Service를
     * 띄우는데, 권한이 없으면 그 자리에서 SecurityException이 난다. 지도 위 안내 시트는 닫을 수
     * 있어서 "닫고 러닝 시작"이 그대로 가능했다.
     *
     * 권한과 위치 서비스는 서로 다른 상태라 순서대로 본다 — 둘 중 하나만 꺼져 있어도 위치를 못 받는다.
     *
     * **코스 탐색에는 걸지 않는다.** 코스를 찾아보는 데까지는 위치가 없어도 되고, 실제로 뛰기
     * 시작할 때만 필요하다.
     */
    var pendingRunStart by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requireLocationForRun: (() -> Unit) -> Unit = { proceed ->
        if (locationPermissionState.isGranted && locationServicesState.isEnabled) {
            proceed()
        } else {
            // 시트에서 허용·켜기를 마치면 이어서 실행한다(아래 LaunchedEffect).
            pendingRunStart = proceed
        }
    }

    // 시트를 띄운 뒤 사용자가 권한을 허용하거나 위치 서비스를 켜면, 다시 누르게 하지 않고 바로 잇는다.
    LaunchedEffect(locationPermissionState.isGranted, locationServicesState.isEnabled, pendingRunStart) {
        val proceed = pendingRunStart ?: return@LaunchedEffect
        if (locationPermissionState.isGranted && locationServicesState.isEnabled) {
            pendingRunStart = null
            proceed()
        }
    }
    val currentDestination by tabNavigator.navController.currentBackStackEntryAsState()
    val tabItems = listOf(
        RunvealTabItemSpec(RunvealIcons.Home, "홈"),
        RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
        RunvealTabItemSpec(RunvealIcons.Record, "기록"),
        RunvealTabItemSpec(RunvealIcons.My, "MY"),
    )

    val selectedIndex = when {
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.HomeMap>() == true -> 0
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommend>() == true -> 0
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Course>() == true -> 1
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.SavedCourseDetail>() == true -> 1
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Record>() == true -> 2
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.RecordDetail>() == true -> 2
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.My>() == true -> 3
        else -> 0
    }
    // 지도를 깔고 그 위에 올라가는 코스 흐름 — 추천(만들기·결과·상세)과 "내 코스"에서 들어가는
    // 코스 상세. 어느 쪽이든 탭바를 숨기고 동 경계·라벨을 걷어 코스에만 집중하게 한다.
    val isCourseFlowOverMap = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommend>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommendResult>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseDetail>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.SavedCourseDetail>() == true
    // 약관 및 정책은 탭 안에서 들어가지만 그 자체로 전체 화면이라 탭바를 숨긴다 — 뒤로가기로
    // 목록·MY 탭으로 돌아온다.
    val isTermsFlow = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Terms>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.TermsDetail>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.ImportRecords>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.ImportRecordsProgress>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.ImportRecordsResult>() == true
    // 기록 상세는 기록 탭 안에서 들어가지만 그 자체로 전체 화면이다 — 탭바까지 덮는다.
    val isRecordDetail = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.RecordDetail>() == true
    // 지도 위 흐름(코스 추천, 코스 추천 결과, 코스 상세, 러닝 기록, 러닝 결과)과 약관 흐름,
    // 기록 상세에 들어가 있을 때만 탭바를 완전히 숨긴다.
    val showTabBar = !isCourseFlowOverMap &&
        !isTermsFlow &&
        !isRecordDetail &&
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Recording>() != true &&
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Result>() != true
    // 지도가 보여야 하는 목적지인지. 지도는 항상 그려 두고(끄면 MapLibre가 매번 스타일을 다시
    // 로드한다) 지도를 쓰지 않는 화면에서만 위를 덮는다 — 그래야 화면 전환 중 양쪽이 반투명해지는
    // 구간에 뒤의 지도가 비치지 않는다. currentDestination이 아직 null인 첫 프레임은 시작 목적지가
    // HomeMap이라 덮지 않는다.
    val isMapDestination = currentDestination == null ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.HomeMap>() == true ||
        isCourseFlowOverMap ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Recording>() == true
    val isZoomedIn = mapState.currentZoom >= MapConfig.ZOOM_IN_THRESHOLD

    // 너무 짧아 저장하지 않은 러닝처럼, 화면 전환만으로는 이유를 알 수 없는 경우에만 띄우는 안내.
    // 토스트 자체는 타이머를 모르는 순수 컴포넌트라 사라지는 건 여기서 재운다.
    val discardedToastMessage = stringResource(R.string.home_recording_discarded_toast)
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage == null) return@LaunchedEffect
        delay(TOAST_DURATION_MS)
        toastMessage = null
    }

    // 지금 서 있는 동 — 조회는 홈 지도 오버레이가 하고(tab-home), 여기선 그 결과만 본다.
    val currentDong by CurrentDongHolder.state.collectAsStateWithLifecycle()

    /**
     * 위치 권한·위치 서비스를 **막 켠 경우** 동 조회를 다시 시킨다.
     *
     * 홈 지도 오버레이는 화면에 들어올 때 한 번만 조회하는데, 첫 실행에서는 권한 창이 아직 떠 있는
     * 동안에 그 한 번을 써 버린다 — 현재 위치를 못 받아 조회를 건너뛰고, 그 화면은 권한 상태를
     * 모르니 허용한 뒤에도 다시 받지 않는다. 그래서 라벨이 "현재 위치"에 그대로 멈춰 있었다.
     *
     * 꺼짐 → 켜짐으로 **바뀔 때만** 보낸다. 처음부터 허용된 상태면 오버레이가 제대로 조회하므로
     * 같은 요청을 두 번 할 이유가 없다.
     */
    var wasLocationReady by remember {
        mutableStateOf(locationPermissionState.isGranted && locationServicesState.isEnabled)
    }
    LaunchedEffect(locationPermissionState.isGranted, locationServicesState.isEnabled) {
        val isLocationReady = locationPermissionState.isGranted && locationServicesState.isEnabled
        if (isLocationReady && !wasLocationReady) CurrentDongHolder.requestRetry()
        wasLocationReady = isLocationReady
    }
    val isDongLoadFailed by CurrentDongHolder.loadFailed.collectAsStateWithLifecycle()
    // 요약 카드 퍼센트는 지도 빛을 정하는 값과 **같은 출처**를 쓴다 — 따로 세면 카드엔 8%인데
    // 지도는 다른 단계로 빛나는 식으로 어긋난다. 아직 안 만들어졌거나 0%면 0.
    val overlayBundle by DongOverlayHolder.state.collectAsStateWithLifecycle()

    /**
     * 탐험률이 가장 높은 행정동 코드 — 앱을 열면 여기로 포커스하고, 요약 카드의 "내 동네" 꼬리표도
     * 이 동에 붙는다. 0%인 동은 [DongOverlayBundle.dongPercents]에 안 들어오므로, 비어 있으면
     * **기록이 아직 없다는 뜻**이다.
     */
    val topExploredDongCode = overlayBundle?.dongPercents?.let(DongExploreRate::topDongCode)

    /**
     * 앱을 열고 **한 번만** 지도를 맞춘다.
     *
     *  - 기록이 있으면 탐험률이 가장 높은 동으로 — 줌은 그대로 둬서 줌아웃 상태를 유지한다.
     *  - 기록이 없으면 현재 위치로.
     *  - 둘 다 없으면 아무것도 하지 않는다 — `recenterToCurrentLocation`은 위치를 못 받으면 조용히
     *    무시되므로 지도가 초기 좌표([MapConfig.SEOUL_CITY_HALL])에 그대로 남는다.
     *
     * 오버레이 묶음을 기다리는 이유는 탐험률과 동 경계가 거기 들어 있어서다 — 스플래시가 미리
     * 만들어 두므로 보통 홈에 들어오는 순간 이미 있다.
     */
    var didInitialFocus by remember { mutableStateOf(false) }
    LaunchedEffect(overlayBundle, didInitialFocus) {
        if (didInitialFocus) return@LaunchedEffect
        val bundle = overlayBundle ?: return@LaunchedEffect
        val topCode = topExploredDongCode
        val center = topCode?.let { code ->
            DongThumbnailGeometry.bounds(bundle.glowPolygonParts[code].orEmpty())?.center
        }
        if (center != null) mapState.moveTo(center) else mapState.recenterToCurrentLocation()
        didInitialFocus = true
    }

    // 요약 카드 썸네일에 쓰는 "가본 길" — 지도 레이어(HomeMapLayer)가 들고 있는 것과 같은 값이지만,
    // 그쪽은 러닝 중 실시간 선으로 바꿔치기해서 밖으로 빼면 얽힌다. 썸네일은 쌓인 길만 필요하므로
    // 기록이 바뀔 때(latestTrackResult)만 따로 읽는다.
    val thumbnailTrackResult by TrackResultHolder.state.collectAsStateWithLifecycle()
    var thumbnailRoadGeoJson by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(thumbnailTrackResult) {
        withContext(Dispatchers.IO) {
            val store = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
            thumbnailRoadGeoJson = DongAssetRepository(context).buildVisitedRoadGeoJson(store.visitedRoadEdges())
        }
    }

    /**
     * 하단 요약 카드가 가리키는 동 — **지도를 움직여도 바뀌지 않는다.**
     *
     * 지도 가운데 동을 따라가게 두면 다른 동네를 둘러보는 동안 "내 동네" 카드가 그 동네 카드로
     * 바뀐다. 어디를 보고 있는지는 좌측 상단 위치 라벨이 알려주므로, 카드는 **내 기준**에 머문다 —
     * 기록이 있으면 탐험률이 가장 높은 동, 없으면 지금 서 있는 동(앱 시작 포커싱과 같은 기준).
     *
     * 이름은 번들에서 꺼낸다([DongOverlayBundle.dongNames]) — 지도 라벨과 같은 이름이고, 좌표로
     * `/api/v1/dong`을 또 부르지 않아도 된다. 둘 다 없으면 null이라 카드가 대체 문구를 쓴다.
     */
    val panelDong = remember(topExploredDongCode, overlayBundle?.dongNames, currentDong) {
        val topCode = topExploredDongCode
        val topName = topCode?.let { overlayBundle?.dongNames?.get(it) }
        // 코드만 있고 이름이 없으면 쓰지 않는다 — 이름과 퍼센트가 다른 동을 가리키게 된다.
        if (topCode != null && topName != null) {
            return@remember HomePanelDong(topCode, topName, isTopExplored = true)
        }
        val dong = currentDong ?: return@remember null
        HomePanelDong(dong.code, dong.dongOnly, isTopExplored = false)
    }

    /**
     * 카드가 가리키는 동의 가운데 좌표 — `>` 아이콘이 여기로 줌인한다. 경계를 아직 못 받았으면
     * null이고, 그때는 아이콘을 그리지 않는다.
     */
    val panelDongCenter = remember(panelDong?.code, overlayBundle?.glowPolygonParts) {
        val code = panelDong?.code ?: return@remember null
        val parts = overlayBundle?.glowPolygonParts?.get(code).orEmpty()
        DongThumbnailGeometry.bounds(parts)?.center
    }

    /**
     * 썸네일 요청 — 동이 바뀌거나 가본 길이 늘거나 표시 방식을 바꿀 때만 다시 만든다.
     *
     * [SnapshotRequest.ownerId]에 동 코드와 기록 ID를 함께 넣어 캐시를 가른다 — 같은 동이라도
     * 새로 달린 길이 생기면 그림이 달라져야 한다.
     */
    val dongThumbnailRequest = remember(
        panelDong?.code,
        thumbnailRoadGeoJson,
        overlayBundle?.glowPolygonParts,
    ) {
        val code = panelDong?.code ?: return@remember null
        val roads = thumbnailRoadGeoJson ?: return@remember null
        val parts = overlayBundle?.glowPolygonParts?.get(code).orEmpty()
        val bounds = DongThumbnailGeometry.bounds(parts) ?: return@remember null
        SnapshotRequest(
            ownerId = "${code}_${thumbnailTrackResult?.trackId ?: "none"}",
            variant = SnapshotVariant.DONG_THUMBNAIL,
            routeGeoJson = roads,
            bounds = bounds,
            start = null,
            end = null,
            // 동 밖은 덮어서 안 보이게 하고, 그 동 경계선은 얹는다.
            dongMaskGeoJson = DongThumbnailGeometry.maskGeoJson(parts, bounds),
            dongOutlineGeoJson = DongThumbnailGeometry.outlineGeoJson(parts),
        )
    }

    /**
     * 서버에서 지도 데이터를 못 받은 상태. 둘 중 하나라도 실패면 참이다 — 동 이름(`/dong`)과 동
     * 경계·탐험률(`/meta` + 지도 파일). 후자가 비면 동 테두리·라벨·빛이 통째로 안 보이고 탐험률이
     * 전부 0%가 되는데, 지금까지는 그게 아무 설명 없이 "원래 그런 화면"처럼 보였다.
     *
     * 오버레이 묶음이 아직 null인 건 **만들고 있는 중**이라 실패로 보지 않는다.
     */
    val isServerDataMissing = isDongLoadFailed || overlayBundle?.isServerDataMissing == true
    var isRetryingServerData by remember { mutableStateOf(false) }
    val retryScope = rememberCoroutineScope()
    val onRetryServerData: () -> Unit = {
        if (!isRetryingServerData) {
            isRetryingServerData = true
            // 동 조회는 다른 모듈(tab-home)이 하므로 홀더로 부탁한다.
            CurrentDongHolder.requestRetry()
            retryScope.launch {
                // 지금 묶음이 반영하고 있던 기록을 그대로 유지한다 — 다른 값을 넣으면 지도 레이어가
                // 묶음을 "낡은 것"으로 보고 또 한 번 다시 만든다.
                refreshDongOverlayFromServer(context, builtForTrackId = overlayBundle?.builtForTrackId)
                isRetryingServerData = false
            }
        }
    }

    val onSelectTab: (Int) -> Unit = { index ->
        val route: RunVealRoute.HomeTab = when (index) {
            0 -> RunVealRoute.HomeTab.Home
            1 -> RunVealRoute.HomeTab.Course
            2 -> RunVealRoute.HomeTab.Record
            else -> RunVealRoute.HomeTab.My
        }
        tabNavigator.navController.navigate(route) {
            popUpTo(tabNavigator.navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(
        LocalMapState provides mapState,
        LocalMapOverlayBottomInset provides
            if (isLocationBannerNeeded || isServerDataMissing) LOCATION_BANNER_EXTRA_INSET else 0.dp,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            HomeMapLayer(
                mapState = mapState,
                locationPermissionState = locationPermissionState,
                hideRegionOverlays = isCourseFlowOverMap,
                modifier = Modifier.fillMaxSize(),
            )

            if (!isMapDestination) {
                Box(modifier = Modifier.fillMaxSize().background(RunvealTheme.colors.bg.canvas))
            }

            NavHost(
                navController = tabNavigator.navController,
                startDestination = RunVealRoute.HomeTab.Home,
                modifier = Modifier.fillMaxSize(),
            ) {
                navigation<RunVealRoute.HomeTab.Home>(startDestination = RunVealRoute.HomeTab.HomeMap) {
                    homeMapOverlay(
                        // 코스 탐색은 위치 없이도 들어갈 수 있다 — 실제로 뛸 때만 위치가 필요하다.
                        onCreateCourseClick = { tabNavigator.navigate(RunVealRoute.HomeTab.CourseRecommend) },
                        onRunStartClick = {
                            requireLocationForRun { tabNavigator.navigate(RunVealRoute.HomeTab.Recording) }
                        },
                    )
                    courseRecommendOverlay(
                        onBack = tabNavigator::popBackStack,
                        onNavigateToResult = { tabNavigator.navigate(RunVealRoute.HomeTab.CourseRecommendResult) },
                    )
                    courseRecommendResultOverlay(
                        onClose = tabNavigator::popBackStack,
                        onCourseClick = { routeId -> tabNavigator.navigate(RunVealRoute.HomeTab.CourseDetail(routeId)) },
                    )
                    courseDetailOverlay(
                        onBack = tabNavigator::popBackStack,
                        // 코스 추천 흐름(CourseRecommend~CourseDetail) 스택을 전부 지우고 들어간다 —
                        // 안 지우면 러닝 종료 후 뒤로가기/닫기로 홈이 아니라 코스 추천 화면으로 돌아간다.
                        // 코스 상세에서 "이 코스로 러닝 시작" — 여기서부터 위치가 필요하다.
                        onStartRunning = {
                            requireLocationForRun {
                                tabNavigator.navController.navigate(RunVealRoute.HomeTab.Recording) {
                                    popUpTo(RunVealRoute.HomeTab.HomeMap) { inclusive = false }
                                }
                            }
                        },
                    )
                    recordingOverlay(
                        onFinished = {
                            tabNavigator.navController.navigate(RunVealRoute.HomeTab.Result) {
                                popUpTo(RunVealRoute.HomeTab.Recording) { inclusive = true }
                            }
                        },
                        // 저장하지 않은 러닝은 결과 화면이 없으니 홈 지도로 되돌리고, 왜 기록이 안
                        // 남았는지 토스트로 알려준다.
                        onDiscarded = {
                            tabNavigator.navController.navigate(RunVealRoute.HomeTab.HomeMap) {
                                popUpTo(RunVealRoute.HomeTab.Recording) { inclusive = true }
                            }
                            toastMessage = discardedToastMessage
                        },
                    )
                    resultOverlay(onClose = tabNavigator::popBackStack)
                }
                courseGraph(tabNavigator)
                savedCourseDetailOverlay(
                    onBack = tabNavigator::popBackStack,
                    onStartRunning = {
                        requireLocationForRun {
                            // 상세를 먼저 떼어낸다 — 러닝이 끝나고 돌아올 자리는 "내 코스" 목록이다.
                            tabNavigator.popBackStack()
                            tabNavigator.navigate(RunVealRoute.HomeTab.Recording)
                        }
                    },
                )
                recordGraph(
                    tabNavigator,
                    // 기록이 없을 때의 "러닝 시작하기" — 홈 탭으로 옮기고 지도를 줌인 뷰까지
                    // 확대한다. 줌인이라야 러닝 시작 버튼이 있는 오버레이가 뜬다
                    // (ZoomedInMapOverlay는 currentZoom >= ZOOM_IN_THRESHOLD에서만 그려진다).
                    //
                    // 기록 탭은 빈 상태가 스택 루트라, MY 탭에서 나올 때처럼 화면을 먼저 떼어낼
                    // 필요가 없다 — 탭만 바꾸면 된다.
                    onStartRunningClick = {
                        onSelectTab(0)
                        mapState.recenterToCurrentLocation(zoom = MapConfig.HOME_ZOOM_IN)
                    },
                )
                myGraph(
                    tabNavigator,
                    onLoggedOut = onLoggedOut,
                    // 기록 불러오기 결과에서 나가는 두 길 — 둘 다 홈 탭으로 넘긴다.
                    //
                    // 탭을 바꾸기 전에 결과 화면을 **MY 탭 스택에서 먼저 떼어낸다.** onSelectTab은
                    // saveState로 떠나는 탭의 스택을 통째로 저장해 뒀다가 돌아올 때 복원하므로,
                    // 그냥 나가면 나중에 MY 탭에 들어올 때 결과 화면이 다시 떠 있다.
                    onCreateCourseClick = {
                        tabNavigator.navController.popBackStack(RunVealRoute.HomeTab.My, inclusive = false)
                        onSelectTab(0)
                        tabNavigator.navigate(RunVealRoute.HomeTab.CourseRecommend)
                    },
                    onGoHomeClick = {
                        tabNavigator.navController.popBackStack(RunVealRoute.HomeTab.My, inclusive = false)
                        onSelectTab(0)
                    },
                )
            }

            toastMessage?.let { message ->
                RunvealToast(
                    message = message,
                    // 시안(Overlay/Toast)은 하단 — 탭바 위 8에 띄운다.
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(
                            start = RunvealTheme.layout.marginScreen,
                            end = RunvealTheme.layout.marginScreen,
                            bottom = RunvealTheme.spacing.space8 + RunvealTheme.layout.sizeTabBar +
                                RunvealTheme.spacing.space8,
                        ),
                )
            }

            if (showTabBar) {
                val locationOffBanner: (@Composable () -> Unit)? = when {
                    !locationPermissionState.isGranted -> {
                        {
                            RunvealLocationOffBanner(
                                message = stringResource(R.string.home_location_permission_off_banner_message),
                                actionLabel = stringResource(R.string.home_location_permission_off_banner_action),
                                // 2회 이상 거부하면 시스템이 권한 다이얼로그를 더 띄워 주지 않는다 —
                                // 그대로 request()를 부르면 아무 일도 안 일어난 것처럼 보인다.
                                // 그때는 앱 설정으로 보낸다(마이페이지의 위치 권한 줄과 같은 처리).
                                onActionClick = {
                                    if (locationPermissionState.isPermanentlyDenied) {
                                        context.openAppSettings()
                                    } else {
                                        locationPermissionState.request()
                                    }
                                },
                            )
                        }
                    }
                    !locationServicesState.isEnabled -> {
                        {
                            RunvealLocationOffBanner(
                                message = stringResource(R.string.home_location_services_off_banner_message),
                                actionLabel = stringResource(R.string.home_location_services_off_banner_action),
                                onActionClick = locationServicesState::requestEnable,
                            )
                        }
                    }
                    // 위치 안내를 먼저 띄운다 — 위치를 못 받으면 동 조회를 아예 건너뛰므로, 위치가
                    // 꺼진 상태에서 "데이터를 받지 못했어요"를 먼저 보여 주면 엉뚱한 안내가 된다.
                    isServerDataMissing -> {
                        {
                            RunvealLocationOffBanner(
                                // 둘 중 뭘 못 받았는지에 따라 문구가 다르다 — 동 이름만 실패했으면
                                // 지도는 제대로 보이는데 "지도 데이터를 못 받았다"는 틀린 안내가 된다.
                                message = stringResource(
                                    when {
                                        isRetryingServerData ->
                                            R.string.home_server_data_error_banner_message_retrying
                                        overlayBundle?.isServerDataMissing == true ->
                                            R.string.home_server_data_error_banner_message_map
                                        else -> R.string.home_server_data_error_banner_message_dong
                                    },
                                ),
                                actionLabel = stringResource(R.string.home_server_data_error_banner_action),
                                onActionClick = onRetryServerData,
                                icon = RunvealIcons.Alert,
                                iconTint = RunvealTheme.colors.status.error,
                            )
                        }
                    }
                    else -> null
                }
                val panelModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(
                        // 패널만 화면 좌우 여백(16)의 예외다 — 시안은 10이다. 패널이 탭바를 좌우 6씩
                        // 감싸는 구조라, 10이어야 탭바가 다른 탭(좌우 16, 너비 328)과 같은 자리에 선다.
                        start = HOME_PANEL_SIDE_MARGIN,
                        end = HOME_PANEL_SIDE_MARGIN,
                        top = RunvealTheme.spacing.space8,
                        bottom = RunvealTheme.spacing.space8 - HOME_BOTTOM_PANEL_INNER_INSET,
                    )

                when {
                    // 홈 탭 · 줌아웃: 패널 전체(배너? + 요약 카드 + 탭바).
                    selectedIndex == 0 && !isZoomedIn -> {
                        RunvealHomeBottomPanel(
                            banner = locationOffBanner,
                            summary = {
                                // 동 이름 옆에 "내 동네" 꼬리표가 붙으므로 이름은 행정동만 쓴다.
                                // 아직 못 받았으면(위치 권한 없음·서비스 지역 밖) 꼬리표 없이
                                // "내 동네"만 — 꼬리표까지 붙이면 "내 동네 내 동네"가 된다.
                                RunvealSummaryCard(
                                    dongName = panelDong?.name
                                        ?: stringResource(R.string.home_summary_my_dong),
                                    ratePercent = panelDong?.code
                                        ?.let { overlayBundle?.dongPercents?.get(it) }
                                        ?: 0,
                                    onClick = {},
                                    // `>`를 누르면 이 카드의 동을 가운데 두고 줌인한다 — 거기서부터
                                    // 도로와 가본 길이 또렷하게 보인다.
                                    onChevronClick = panelDongCenter?.let { center ->
                                        { mapState.moveTo(center, MapConfig.HOME_ZOOM_IN) }
                                    },
                                    // 기록이 있어 탐험률 최고 동을 가리키면 "내 동네", 기록이 없어
                                    // 지금 서 있는 동을 가리키면 "현위치".
                                    tagLabel = when {
                                        panelDong == null -> null
                                        panelDong.isTopExplored ->
                                            stringResource(R.string.home_summary_tag_my_dong)
                                        else -> stringResource(R.string.home_summary_tag_current_location)
                                    },
                                ) {
                                    // 요청이 아직 없으면(동/가본 길/경계 중 하나라도 미도착) 빈
                                    // 프레임만 보인다 — CourseSnapshotImage가 placeholder를 그린다.
                                    CourseSnapshotImage(
                                        request = dongThumbnailRequest,
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize(),
                                    )
                                }
                            },
                            tabBar = {
                                RunvealTabBar(
                                    items = tabItems,
                                    selectedIndex = selectedIndex,
                                    onSelect = onSelectTab,
                                    container = RunvealTabBarContainer.Embedded,
                                )
                            },
                            onMyLocationClick = mapState::recenterToCurrentLocation,
                            modifier = panelModifier,
                        )
                    }
                    // 홈 탭 · 줌인 + 위치 꺼짐: 요약 카드는 생략하고 배너만 탭바 위에 얹는다.
                    selectedIndex == 0 && isZoomedIn && locationOffBanner != null -> {
                        RunvealHomeBottomPanel(
                            banner = locationOffBanner,
                            tabBar = {
                                RunvealTabBar(
                                    items = tabItems,
                                    selectedIndex = selectedIndex,
                                    onSelect = onSelectTab,
                                    container = RunvealTabBarContainer.Embedded,
                                )
                            },
                            modifier = panelModifier,
                        )
                    }
                    // 그 외(줌인 + 위치 정상, 다른 탭): 탭바만 홀로 띄운다.
                    else -> {
                        RunvealTabBar(
                            items = tabItems,
                            selectedIndex = selectedIndex,
                            onSelect = onSelectTab,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                                .padding(
                                    horizontal = RunvealTheme.layout.marginScreen + HOME_BOTTOM_PANEL_INNER_INSET,
                                    vertical = RunvealTheme.spacing.space8,
                                ),
                        )
                    }
                }
            }

            // 러닝 시작을 눌렀는데 위치가 준비 안 된 경우 — 무엇이 모자란지에 맞는 시트를 띄운다.
            // 권한을 먼저 보고, 권한이 있으면 위치 서비스를 본다(둘 중 하나만 꺼져도 위치를 못 받는다).
            if (pendingRunStart != null) {
                ModalBottomSheet(
                    onDismissRequest = { pendingRunStart = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.Transparent,
                    dragHandle = null,
                    // 시트 배경이 OS 내비게이션 바 뒤까지 그대로 채우게 한다(RequireLocationPermission과 같은 이유).
                    contentWindowInsets = { WindowInsets(0) },
                ) {
                    when {
                        locationPermissionState.isPermanentlyDenied ->
                            RunvealLocationPermissionSettingsSheet(
                                onSettingsClick = { context.openAppSettings() },
                                onCloseClick = { pendingRunStart = null },
                            )

                        !locationPermissionState.isGranted ->
                            RunvealLocationPermissionSheet(
                                onAllowClick = locationPermissionState::request,
                                onLaterClick = { pendingRunStart = null },
                            )

                        else ->
                            RunvealLocationServicesSheet(
                                onSettingsClick = locationServicesState::requestEnable,
                                onCloseClick = { pendingRunStart = null },
                            )
                    }
                }
            }
        }
    }
}

/** Map Shell이 그리는 지도 자체. 위치 권한 요청도 여기서 끝낸다(지도는 항상 표시, 미승인 시 안내 바텀시트를 위에 띄움).
 * [hideRegionOverlays]는 코스 추천(만들기/결과) 흐름처럼 녹화 중이 아니어도 동/시군구/시도 레이어를
 * 숨겨야 하는 화면에서 true로 준다. */
@Composable
private fun HomeMapLayer(
    mapState: MapState,
    locationPermissionState: LocationPermissionState,
    hideRegionOverlays: Boolean,
    modifier: Modifier = Modifier,
) {
    RequireLocationPermission(modifier = modifier, state = locationPermissionState) {
        val context = LocalContext.current
        val repository = remember { DongAssetRepository(context) }
        val trackLocalStore = remember { TrackLocalStore(RunvealDatabaseProvider.database, FileStore()) }
        // TrackResultHolder가 매칭 완료마다 새 값을 publish하므로, 그걸 키로 "가본 길"/동별 완주율을
        // DB에서 다시 읽어 새 기록이 반영되게 한다.
        val latestTrackResult by TrackResultHolder.state.collectAsStateWithLifecycle()

        val emptyVisitedRoadGeoJson = remember { repository.buildVisitedRoadGeoJson(emptyList()) }
        var visitedRoadGeoJson by remember { mutableStateOf(emptyVisitedRoadGeoJson) }
        LaunchedEffect(latestTrackResult) {
            withContext(Dispatchers.IO) {
                visitedRoadGeoJson = repository.buildVisitedRoadGeoJson(trackLocalStore.visitedRoadEdges())
            }
        }

        // 기록 중엔 지금까지 쌓인 "가본 길" 대신 지금 달리고 있는 실시간 경로를 보여준다 — 이미 가본
        // 구간(흰색)과 처음 가보는 구간(금색)을 [LiveRoadClassifier]로 나눠서 각각 따로 그린다
        // (전부 로컬 판정, docs/api/API.md §3.5). TrackingService가 10초마다 publish하는 도로 매칭
        // 선([LiveMatchHolder])이 있으면 그걸 쓰고, 아직 없으면(러닝 시작 직후 등) TrackRecorder가
        // 들고 있는 원본 GPS 점으로 대신 분류한다. TrackRecorder.state는 점이 추가될 때마다 새로
        // emit되지 않아서(§3 설계) 1초마다 직접 읽어 다시 그린다 — RecordingViewModel의 경과시간
        // 틱과 같은 패턴.
        val recordingState by TrackRecorder.state.collectAsStateWithLifecycle()
        val isRecordingOrPaused = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused
        val liveRoadClassifier = remember { LiveRoadClassifier(trackLocalStore.database) }
        var liveRunOldGeoJson by remember { mutableStateOf(emptyVisitedRoadGeoJson) }
        var liveRunNewGeoJson by remember { mutableStateOf(emptyVisitedRoadGeoJson) }
        LaunchedEffect(isRecordingOrPaused) {
            if (!isRecordingOrPaused) return@LaunchedEffect
            // 직전 러닝에서 적립된 구간까지 반영해 "이미 가본 길"을 다시 읽는다 — 안 그러면 앱을 켠 채
            // 두 번째로 달릴 때 방금 달린 길이 또 처음 가는 길(금색)로 보인다.
            withContext(Dispatchers.IO) { liveRoadClassifier.refreshExploredSegments() }
            while (true) {
                withContext(Dispatchers.IO) {
                    // 매칭 경로는 GPS와 동떨어진 부분을 잘라낸 **조각 목록**이라(MatchedPathFilter),
                    // 조각마다 따로 분류하고 따로 그린다 — 이어 붙이면 잘라낸 구간이 직선으로 되살아난다.
                    val livePaths = LiveMatchHolder.state.value
                        ?: listOf(TrackRecorder.currentFilteredPoints().map { LatLon(lat = it.lat, lon = it.lon) })
                    val classified = livePaths.map { liveRoadClassifier.classify(it) }
                    liveRunOldGeoJson = buildVisitedRoadGeoJson(classified.flatMap { it.oldRuns })
                    liveRunNewGeoJson = buildVisitedRoadGeoJson(classified.flatMap { it.newRuns })
                    // 기록 화면의 "새로 탐험한 길" 배지가 같은 분류 결과를 쓰게 넘긴다 — 분류는 전국
                    // 구간 인덱스를 훑어서, 화면마다 따로 돌리면 매초 같은 계산을 두 번 하게 된다.
                    LiveNewRoadHolder.publish(classified.sumOf { it.newRoadMeters })
                }
                delay(1000)
            }
        }
        MapLayers(
            MapLayerSpec(MapConfig.SOURCE_VISITED_ROAD, if (isRecordingOrPaused) emptyVisitedRoadGeoJson else visitedRoadGeoJson),
            MapLayerSpec(MapConfig.SOURCE_LIVE_RUN_OLD, if (isRecordingOrPaused) liveRunOldGeoJson else emptyVisitedRoadGeoJson),
            MapLayerSpec(MapConfig.SOURCE_LIVE_RUN_NEW, if (isRecordingOrPaused) liveRunNewGeoJson else emptyVisitedRoadGeoJson),
        )

        // 동/시군구/시도 오버레이는 스플래시가 미리 만들어 [DongOverlayHolder]에 담아 둔다 — 전국 경계
        // geoJSON 파싱이 몇 초 걸려서 홈에 들어온 뒤에 시작하면 지도가 한참 비어 있다가 뒤늦게 동이
        // 떠오른다. 여기선 담긴 걸 그대로 쓰고, 러닝을 끝냈을 때만(= 담긴 묶음이 그 기록을 아직 반영
        // 못 했을 때만) 다시 만들어 홀더를 갈아 끼운다.
        val overlayBundle by DongOverlayHolder.state.collectAsStateWithLifecycle()
        LaunchedEffect(latestTrackResult) {
            if (overlayBundle != null && overlayBundle?.builtForTrackId == latestTrackResult?.trackId) return@LaunchedEffect
            DongOverlayHolder.publish(loadDongOverlayBundle(context, builtForTrackId = latestTrackResult?.trackId))
        }

        // 기록 중이거나 코스 추천(만들기/결과) 중엔 줌아웃해도 동/시군구/시도 테두리·라벨·빛을 안
        // 보여준다 — 지금 뛰고 있는 길, 또는 지금 고르고 있는 코스에만 집중하게 한다. 같은 이유로
        // 베이스 지도도 줌아웃해도 "줌인 상태" 밝기로 고정한다(keepZoomedInStyle) — 동 경계를 안
        // 띄우니 흐리게 할 이유가 없고, 어두워지면 길이 안 보여 코스를 가늠할 수 없다.
        val hideOverlays = isRecordingOrPaused || hideRegionOverlays
        RunvealMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            dongOverlay = if (hideOverlays) null else overlayBundle?.dongOverlay,
            dongGlowPolygonParts = if (hideOverlays) emptyMap() else overlayBundle?.glowPolygonParts.orEmpty(),
            dongBoundarySourceKey = overlayBundle?.boundarySourceKey.orEmpty(),
            sigunguOverlay = if (hideOverlays) null else overlayBundle?.sigunguOverlay,
            sidoOverlay = if (hideOverlays) null else overlayBundle?.sidoOverlay,
            locationPermissionGranted = locationPermissionState.isGranted,
            keepZoomedInStyle = hideOverlays,
        )
    }
}

/**
 * 홈 줌아웃 하단 요약 카드가 가리키는 동.
 *
 * 코드·이름을 같이 들고 다니는 이유: 이름은 오버레이 번들에서, 퍼센트는 탐험률 집계에서 꺼내는데
 * 둘을 따로 고르면 "이름은 A동인데 퍼센트는 B동" 같은 상태가 생긴다.
 *
 * [isTopExplored]가 참이면 기록 기준(탐험률이 가장 높은 동)이라 꼬리표가 "내 동네", 거짓이면
 * 기록이 없어 지금 서 있는 동을 가리키는 것이라 "현위치"다.
 */
private data class HomePanelDong(
    val code: String,
    val name: String,
    val isTopExplored: Boolean,
)
