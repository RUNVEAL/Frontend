package com.swyp.runveal.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.home.HomeScreen
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute

fun NavGraphBuilder.homeGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Home.Main> {
        HomeScreen()
    }
}
