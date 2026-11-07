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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.core.map.MapState
import com.swyp.runveal.core.map.RunvealMap
import com.swyp.runveal.core.map.rememberMapState
import com.swyp.runveal.courserecommend.navigation.courseRecommendOverlay
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.swyp.runveal.designsystem.component.card.RunvealSummaryCard
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.map.MapConfig
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.navigation.rememberRunVealNavigator
import com.swyp.runveal.permission.RequireLocationPermission
import com.swyp.runveal.tabcourse.navigation.courseGraph
import com.swyp.runveal.tabhome.navigation.homeMapOverlay
import com.swyp.runveal.tabmypage.navigation.myGraph
import com.swyp.runveal.tabrecord.navigation.recordGraph

private val HOME_BOTTOM_PANEL_INNER_INSET = 6.dp

/**
 * 홈 탭 화면 — Map Shell. 지도(`RunvealMap`)는 여기서 하나만 그리고, 탭 화면과 지도 위 흐름(홈 지도,
 * 코스 추천)은 전부 이 아래 단일 NavHost로 조립한다. 탭바 노출 규칙도 여기 한 곳에만 둔다.
 */
@Composable
internal fun HomeScreen(modifier: Modifier = Modifier) {
    val tabNavigator = rememberRunVealNavigator()
    val mapState = rememberMapState()
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
    // 지도 위 흐름(코스 추천)에 들어가 있을 때만 탭바를 완전히 숨긴다.
    val showTabBar = currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.CourseRecommend>() != true
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

    CompositionLocalProvider(LocalMapState provides mapState) {
        Box(modifier = modifier.fillMaxSize()) {
            HomeMapLayer(mapState = mapState, modifier = Modifier.fillMaxSize())

            NavHost(
                navController = tabNavigator.navController,
                startDestination = RunVealRoute.HomeTab.Home,
                modifier = Modifier.fillMaxSize(),
            ) {
                navigation<RunVealRoute.HomeTab.Home>(startDestination = RunVealRoute.HomeTab.HomeMap) {
                    homeMapOverlay(
                        onCreateCourseClick = { tabNavigator.navigate(RunVealRoute.HomeTab.CourseRecommend) },
                    )
                    courseRecommendOverlay(onBack = tabNavigator::popBackStack)
                }
                courseGraph(tabNavigator)
                recordGraph(tabNavigator)
                myGraph(tabNavigator)
            }

            if (showTabBar) {
                if (selectedIndex == 0 && !isZoomedIn) {
                    RunvealHomeBottomPanel(
                        summary = {
                            // TODO: 위치 권한 처리, 러닝 기록 연동 추가 예정
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            .padding(
                                start = RunvealTheme.layout.marginScreen,
                                end = RunvealTheme.layout.marginScreen,
                                top = RunvealTheme.spacing.space8,
                                bottom = RunvealTheme.spacing.space8 - HOME_BOTTOM_PANEL_INNER_INSET,
                            ),
                    )
                } else {
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

/** Map Shell이 그리는 지도 자체. 위치 권한 요청도 여기서 끝낸다(지도는 항상 표시, 미승인 시 안내 바텀시트를 위에 띄움). */
@Composable
private fun HomeMapLayer(mapState: MapState, modifier: Modifier = Modifier) {
    RequireLocationPermission(modifier = modifier) {
        RunvealMap(modifier = Modifier.fillMaxSize(), state = mapState)
    }
}
