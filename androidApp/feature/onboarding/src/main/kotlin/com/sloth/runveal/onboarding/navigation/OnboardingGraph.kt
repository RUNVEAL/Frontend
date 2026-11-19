package com.sloth.runveal.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.onboarding.OnboardingScreen

fun NavGraphBuilder.onboardingGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Main.Onboarding> {
        OnboardingScreen(
            // 한 번만 보는 화면이라 백스택에서 치운다 — 홈에서 뒤로 눌러 돌아오면 안 된다.
            onFinished = { navigator.navigateClearingBackStack(RunVealRoute.Home.Main) },
        )
    }
}
