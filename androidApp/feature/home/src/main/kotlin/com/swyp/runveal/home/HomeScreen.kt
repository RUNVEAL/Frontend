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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.home.navigation.homeTabGraph
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.navigation.rememberRunVealNavigator

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
        HomeMapScreen(modifier = Modifier.fillMaxSize())

        NavHost(
            navController = tabNavigator.navController,
            startDestination = RunVealRoute.HomeTab.Home,
            modifier = Modifier.fillMaxSize(),
        ) {
            homeTabGraph(navigator = tabNavigator)
        }

        RunvealTabBar(
            items = tabItems,
            selectedIndex = selectedIndex,
            onSelect = onSelectTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space8),
        )
    }
}
