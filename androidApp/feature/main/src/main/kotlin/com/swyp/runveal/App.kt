package com.swyp.runveal

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.home.navigation.homeGraph
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.navigation.rememberRunVealNavigator
import com.swyp.runveal.splash.splashGraph

@Composable
@Preview
internal fun App(
    navigator: RunVealNavigator = rememberRunVealNavigator()
) {
    RunvealTheme {
        NavHost(
            navController = navigator.navController,
            startDestination = RunVealRoute.Home.Main,
        ) {
            splashGraph(navigator = navigator)
            homeGraph(navigator = navigator)
        }
    }
}
