package com.sloth.runveal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import com.sloth.runveal.designsystem.component.overlay.RunvealLocationServicesSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealLocationPermissionSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealLocationPermissionSettingsSheet
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
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
import com.sloth.runveal.core.map.rememberMapState
import com.sloth.runveal.courserecommend.navigation.courseDetailOverlay
import com.sloth.runveal.courserecommend.navigation.courseRecommendOverlay
import com.sloth.runveal.courserecommend.navigation.courseRecommendResultOverlay
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.sloth.runveal.designsystem.component.card.RunvealLocationOffBanner
import com.sloth.runveal.designsystem.component.card.RunvealSummaryCard
import com.sloth.runveal.designsystem.component.navigation.RunvealTabBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.sloth.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.sloth.runveal.designsystem.component.overlay.RunvealToast
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.network.dong.CurrentDongHolder
import com.sloth.runveal.navigation.rememberRunVealNavigator
import com.sloth.runveal.feature.home.R
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.permission.LocationPermissionState
import com.sloth.runveal.permission.RequireLocationPermission
import com.sloth.runveal.permission.openAppSettings
import com.sloth.runveal.devicestate.rememberLocationServicesState
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
import kotlinx.coroutines.withContext

private val HOME_BOTTOM_PANEL_INNER_INSET = 6.dp

/** 토스트가 떠 있는 시간 — 디자인시스템 토스트 주석의 "3초"를 그대로 따른다. */
private const val TOAST_DURATION_MS = 3000L

/**
 * 줌인 오버레이의 하단 액션 줄이 위치 안내 배너 때문에 더 높아진 패널과 안 겹치게 추가로 띄워야 하는
 * 높이. 배너(40dp) + 패널 안쪽 top padding(12dp) + 배너·탭바 사이 gap(4dp) - 패널이 대신하는
 * 탭바 단독 바깥 padding(16dp) = 56dp. 매칭되는 토큰이 없어 직접 계산해 하드코딩했다.
 */
private val LOCATION_BANNER_EXTRA_INSET = 56.dp

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
    val appSettingsContext = LocalContext.current

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
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Record>() == true -> 2
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.My>() == true -> 3
        else -> 0
    }
    val isCourseRecommendFlow = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommend>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommendResult>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseDetail>() == true
    // 약관 및 정책은 탭 안에서 들어가지만 그 자체로 전체 화면이라 탭바를 숨긴다 — 뒤로가기로
    // 목록·MY 탭으로 돌아온다.
    val isTermsFlow = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Terms>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.TermsDetail>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.ImportRecords>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.ImportRecordsProgress>() == true ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.ImportRecordsResult>() == true
    // 지도 위 흐름(코스 추천, 코스 추천 결과, 코스 상세, 러닝 기록, 러닝 결과)과 약관 흐름에
    // 들어가 있을 때만 탭바를 완전히 숨긴다.
    val showTabBar = !isCourseRecommendFlow &&
        !isTermsFlow &&
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Recording>() != true &&
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Result>() != true
    // 지도가 보여야 하는 목적지인지. 지도는 항상 그려 두고(끄면 MapLibre가 매번 스타일을 다시
    // 로드한다) 지도를 쓰지 않는 화면에서만 위를 덮는다 — 그래야 화면 전환 중 양쪽이 반투명해지는
    // 구간에 뒤의 지도가 비치지 않는다. currentDestination이 아직 null인 첫 프레임은 시작 목적지가
    // HomeMap이라 덮지 않는다.
    val isMapDestination = currentDestination == null ||
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.HomeMap>() == true ||
        isCourseRecommendFlow ||
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
    // 요약 카드 퍼센트는 지도 빛을 정하는 값과 **같은 출처**를 쓴다 — 따로 세면 카드엔 8%인데
    // 지도는 다른 단계로 빛나는 식으로 어긋난다. 아직 안 만들어졌거나 0%면 0.
    val overlayBundle by DongOverlayHolder.state.collectAsStateWithLifecycle()

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
        LocalMapOverlayBottomInset provides if (isLocationBannerNeeded) LOCATION_BANNER_EXTRA_INSET else 0.dp,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            HomeMapLayer(
                mapState = mapState,
                locationPermissionState = locationPermissionState,
                hideRegionOverlays = isCourseRecommendFlow,
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
                recordGraph(tabNavigator)
                myGraph(
                    tabNavigator,
                    onLoggedOut = onLoggedOut,
                    // 기록 불러오기 결과에서 나가는 두 길 — 둘 다 홈 탭으로 넘긴다. onSelectTab이
                    // 시작 목적지까지 되감아서 결과 화면은 스택에서 빠진다.
                    onCreateCourseClick = {
                        onSelectTab(0)
                        tabNavigator.navigate(RunVealRoute.HomeTab.CourseRecommend)
                    },
                    onGoHomeClick = { onSelectTab(0) },
                )
            }

            toastMessage?.let { message ->
                RunvealToast(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space16),
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
                                        appSettingsContext.openAppSettings()
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
                    else -> null
                }
                val panelModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(
                        start = RunvealTheme.layout.marginScreen,
                        end = RunvealTheme.layout.marginScreen,
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
                                    dongName = currentDong?.dongOnly
                                        ?: stringResource(R.string.home_summary_my_dong),
                                    ratePercent = currentDong?.code
                                        ?.let { overlayBundle?.dongPercents?.get(it) }
                                        ?: 0,
                                    onClick = {},
                                    isMine = currentDong != null,
                                )
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
                                onSettingsClick = { appSettingsContext.openAppSettings() },
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
        // 보여준다 — 지금 뛰고 있는 길, 또는 지금 고르고 있는 코스에만 집중하게 한다.
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
        )
    }
}
