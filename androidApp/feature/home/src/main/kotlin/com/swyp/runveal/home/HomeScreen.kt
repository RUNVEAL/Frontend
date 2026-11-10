package com.swyp.runveal.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
import com.swyp.runveal.core.map.DongAssetRepository
import com.swyp.runveal.core.map.LocalMapOverlayBottomInset
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.core.map.MapLayerSpec
import com.swyp.runveal.core.map.MapLayers
import com.swyp.runveal.core.map.MapState
import com.swyp.runveal.core.map.RunvealMap
import com.swyp.runveal.core.map.buildVisitedRoadGeoJson
import com.swyp.runveal.core.map.rememberMapState
import com.swyp.runveal.courserecommend.navigation.courseRecommendOverlay
import com.swyp.runveal.data.local.DatabaseDriverFactory
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.swyp.runveal.designsystem.component.card.RunvealLocationOffBanner
import com.swyp.runveal.designsystem.component.card.RunvealSummaryCard
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.map.MapConfig
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.navigation.rememberRunVealNavigator
import com.swyp.runveal.feature.home.R
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.permission.LocationPermissionState
import com.swyp.runveal.permission.RequireLocationPermission
import com.swyp.runveal.permission.rememberLocationPermissionState
import com.swyp.runveal.recording.RecordingState
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.result.TrackResultHolder
import com.swyp.runveal.running.navigation.recordingOverlay
import com.swyp.runveal.running.navigation.resultOverlay
import com.swyp.runveal.tabcourse.navigation.courseGraph
import com.swyp.runveal.tabhome.navigation.homeMapOverlay
import com.swyp.runveal.tabmypage.navigation.myGraph
import com.swyp.runveal.tabrecord.navigation.recordGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val HOME_BOTTOM_PANEL_INNER_INSET = 6.dp

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
@Composable
internal fun HomeScreen(modifier: Modifier = Modifier) {
    val tabNavigator = rememberRunVealNavigator()
    val mapState = rememberMapState()
    val locationPermissionState = rememberLocationPermissionState()
    val isLocationBannerNeeded = !locationPermissionState.isGranted || !locationPermissionState.isLocationServicesEnabled
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
    // 지도 위 흐름(코스 추천, 러닝 기록, 러닝 결과)에 들어가 있을 때만 탭바를 완전히 숨긴다.
    val showTabBar = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommend>() != true &&
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Recording>() != true &&
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Result>() != true
    val isZoomedIn = mapState.currentZoom >= MapConfig.ZOOM_IN_THRESHOLD

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
            HomeMapLayer(mapState = mapState, locationPermissionState = locationPermissionState, modifier = Modifier.fillMaxSize())

            NavHost(
                navController = tabNavigator.navController,
                startDestination = RunVealRoute.HomeTab.Home,
                modifier = Modifier.fillMaxSize(),
            ) {
                navigation<RunVealRoute.HomeTab.Home>(startDestination = RunVealRoute.HomeTab.HomeMap) {
                    homeMapOverlay(
                        onCreateCourseClick = { tabNavigator.navigate(RunVealRoute.HomeTab.CourseRecommend) },
                        onRunStartClick = { tabNavigator.navigate(RunVealRoute.HomeTab.Recording) },
                    )
                    courseRecommendOverlay(onBack = tabNavigator::popBackStack)
                    recordingOverlay(
                        onFinished = {
                            tabNavigator.navController.navigate(RunVealRoute.HomeTab.Result) {
                                popUpTo(RunVealRoute.HomeTab.Recording) { inclusive = true }
                            }
                        },
                    )
                    resultOverlay(onClose = tabNavigator::popBackStack)
                }
                courseGraph(tabNavigator)
                recordGraph(tabNavigator)
                myGraph(tabNavigator)
            }

            if (showTabBar) {
                val locationOffBanner: (@Composable () -> Unit)? = when {
                    !locationPermissionState.isGranted -> {
                        {
                            RunvealLocationOffBanner(
                                message = stringResource(R.string.home_location_permission_off_banner_message),
                                actionLabel = stringResource(R.string.home_location_permission_off_banner_action),
                                onActionClick = locationPermissionState::request,
                            )
                        }
                    }
                    !locationPermissionState.isLocationServicesEnabled -> {
                        {
                            RunvealLocationOffBanner(
                                message = stringResource(R.string.home_location_services_off_banner_message),
                                actionLabel = stringResource(R.string.home_location_services_off_banner_action),
                                onActionClick = locationPermissionState::requestEnableLocationServices,
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
                                // TODO: 러닝 기록 연동 추가 예정
                                RunvealSummaryCard(
                                    dongName = "내 동네",
                                    ratePercent = 0,
                                    onClick = {},
                                    isMine = true
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
        }
    }
}

/** Map Shell이 그리는 지도 자체. 위치 권한 요청도 여기서 끝낸다(지도는 항상 표시, 미승인 시 안내 바텀시트를 위에 띄움). */
@Composable
private fun HomeMapLayer(mapState: MapState, locationPermissionState: LocationPermissionState, modifier: Modifier = Modifier) {
    RequireLocationPermission(modifier = modifier, state = locationPermissionState) {
        val context = LocalContext.current
        val repository = remember { DongAssetRepository(context) }
        val trackLocalStore = remember { TrackLocalStore(RunvealDatabase(DatabaseDriverFactory().createDriver()), FileStore()) }
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

        // 기록 중엔 지금까지 쌓인 "가본 길" 대신 지금 달리고 있는 실시간 경로를 보여준다 — TrackRecorder가
        // 이미 들고 있는 점들로 그린다. TrackRecorder.state는 점이 추가될 때마다 새로 emit되지
        // 않아서(§3 설계) 1초마다 직접 읽어 다시 그린다 — RecordingViewModel의 경과시간 틱과 같은 패턴.
        val recordingState by TrackRecorder.state.collectAsStateWithLifecycle()
        val isRecordingOrPaused = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused
        var liveTrackGeoJson by remember { mutableStateOf(emptyVisitedRoadGeoJson) }
        LaunchedEffect(isRecordingOrPaused) {
            if (!isRecordingOrPaused) return@LaunchedEffect
            while (true) {
                val livePoints = TrackRecorder.currentFilteredPoints().map { LatLon(lat = it.lat, lon = it.lon) }
                liveTrackGeoJson = buildVisitedRoadGeoJson(listOf(livePoints))
                delay(1000)
            }
        }
        MapLayers(MapLayerSpec(MapConfig.SOURCE_VISITED_ROAD, if (isRecordingOrPaused) liveTrackGeoJson else visitedRoadGeoJson))

        val dongOverlayData = remember { repository.buildOverlay(edges = emptyList()) }
        val dongGlowPolygonPartsData = remember { repository.loadPolygonParts() }
        val sigunguOverlayData = remember { repository.buildSigunguOverlay(edges = emptyList()) }
        val sidoOverlayData = remember { repository.buildSidoOverlay(edges = emptyList()) }

        // 기록 중엔 줌아웃해도 동/시군구/시도 테두리·라벨·빛을 안 보여준다 — 지금 뛰고 있는 길에만
        // 집중하게 한다.
        RunvealMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            dongOverlay = if (isRecordingOrPaused) null else dongOverlayData,
            dongGlowPolygonParts = if (isRecordingOrPaused) emptyMap() else dongGlowPolygonPartsData,
            sigunguOverlay = if (isRecordingOrPaused) null else sigunguOverlayData,
            sidoOverlay = if (isRecordingOrPaused) null else sidoOverlayData,
        )
    }
}
