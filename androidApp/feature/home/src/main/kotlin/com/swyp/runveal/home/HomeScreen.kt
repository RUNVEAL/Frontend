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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.swyp.runveal.designsystem.component.card.RunvealSummaryCard
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.navigation.rememberRunVealNavigator
import com.swyp.runveal.tabcourse.navigation.courseGraph
import com.swyp.runveal.tabhome.HomeMapScreen
import com.swyp.runveal.tabhome.navigation.tabHomeGraph
import com.swyp.runveal.tabmypage.navigation.myGraph
import com.swyp.runveal.tabrecord.navigation.recordGraph

private val HOME_BOTTOM_PANEL_INNER_INSET = 6.dp

@Composable
internal fun HomeScreen(modifier: Modifier = Modifier) {
    val tabNavigator = rememberRunVealNavigator()
    val currentDestination by tabNavigator.navController.currentBackStackEntryAsState()
    val tabItems = listOf(
        RunvealTabItemSpec(RunvealIcons.Home, "홈"),
        RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
        RunvealTabItemSpec(RunvealIcons.Record, "기록"),
        RunvealTabItemSpec(RunvealIcons.My, "MY"),
    )

    val selectedIndex = when {
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Home>() == true -> 0
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Course>() == true -> 1
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.Record>() == true -> 2
        currentDestination?.destination?.hasRoute<RunVealRoute.HomeTab.My>() == true -> 3
        else -> 0
    }

    var isZoomedIn by remember { mutableStateOf(false) }

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

    Box(modifier = modifier.fillMaxSize()) {
        HomeMapScreen(
            modifier = Modifier.fillMaxSize(),
            onZoomedInChanged = { isZoomedIn = it },
        )

        NavHost(
            navController = tabNavigator.navController,
            startDestination = RunVealRoute.HomeTab.Home,
            modifier = Modifier.fillMaxSize(),
        ) {
            tabHomeGraph(tabNavigator)
            courseGraph(tabNavigator)
            recordGraph(tabNavigator)
            myGraph(tabNavigator)
        }

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
