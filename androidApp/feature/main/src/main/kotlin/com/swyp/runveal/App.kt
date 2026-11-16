package com.swyp.runveal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.home.navigation.homeGraph
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.navigation.rememberRunVealNavigator
import com.swyp.runveal.sign.signin.navigation.signInGraph
import com.swyp.runveal.sign.signup.navigation.nicknameGraph
import com.swyp.runveal.sign.signup.navigation.termsGraph
import com.swyp.runveal.splash.splashGraph
import com.swyp.runveal.ui.network.NetworkErrorOverlay

@Composable
@Preview
internal fun App(
    navigator: RunVealNavigator = rememberRunVealNavigator()
) {
    RunvealTheme {
        // NavHost 자체는 화면 전환 중 두 목적지가 겹치지 않는 순간이 있어 그때 창 배경이 드러난다.
        // 뒤에 bg/canvas를 깔아 스플래시 → 로그인/홈 전환에서 밝은 색이 번쩍이지 않게 한다.
        // (insets는 소비하지 않는다 — CLAUDE.md UI 컨벤션)
        Box(modifier = Modifier.fillMaxSize().background(RunvealTheme.colors.bg.canvas)) {
            NavHost(
                navController = navigator.navController,
                startDestination = RunVealRoute.Main.Splash,
            ) {
                splashGraph(navigator = navigator)
                signInGraph(navigator = navigator)
                termsGraph(navigator = navigator)
                nicknameGraph(navigator = navigator)
                homeGraph(navigator = navigator)
            }

            // 어느 화면에 있든 네트워크가 끊기면 그 위를 덮는다. 화면마다 붙이지 않고 여기 한 번만
            // 둔다 — 뒤로가기로 닫으면 원래 화면으로 돌아간다(NetworkErrorOverlay 주석).
            NetworkErrorOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}
