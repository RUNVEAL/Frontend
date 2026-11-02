package com.swyp.runveal.splash

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MILLIS = 2000L

internal fun NavGraphBuilder.splashGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Main.Splash> {
        LaunchedEffect(Unit) {
            delay(SPLASH_DURATION_MILLIS)

            // TODO: 로그인 여부 확인 후 로그인 화면/홈으로 분기. 로그인 기능 구현 전까지는 항상 홈으로 이동.
            navigator.navigate(
                route = RunVealRoute.Home.Main,
                navOptions = navOptions {
                    popUpTo<RunVealRoute.Main.Splash> { inclusive = true }
                },
            )
        }

        SplashScreen()
    }
}
