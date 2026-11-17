package com.swyp.runveal.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.home.HomeScreen
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute

fun NavGraphBuilder.homeGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Home.Main> {
        HomeScreen(
            // 로그아웃하면 홈을 포함한 백스택을 전부 비우고 로그인으로 — 뒤로 눌러 돌아올 수 없어야 한다.
            onLoggedOut = { navigator.navigateClearingBackStack(RunVealRoute.Sign.SignIn) },
        )
    }
}
