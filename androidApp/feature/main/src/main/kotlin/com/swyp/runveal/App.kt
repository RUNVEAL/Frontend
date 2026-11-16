package com.swyp.runveal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.devicestate.NetworkErrorOverlay
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
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navigator.navController,
                startDestination = RunVealRoute.Main.Splash,
            ) {
                splashGraph(navigator = navigator)
                homeGraph(navigator = navigator)
            }

            // 어느 화면에 있든 네트워크가 끊기면 그 위를 덮는다. 화면마다 붙이지 않고 여기 한 번만
            // 둔다 — 뒤로가기로 닫으면 원래 화면으로 돌아간다(NetworkErrorOverlay 주석).
            NetworkErrorOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}
