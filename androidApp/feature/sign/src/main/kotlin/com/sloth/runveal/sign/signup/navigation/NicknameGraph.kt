package com.sloth.runveal.sign.signup.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.sign.signup.NicknameScreen

fun NavGraphBuilder.nicknameGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Sign.Nickname> {
        NicknameScreen(
            onBackClick = { navigator.navigateUp() },
            // 가입이 끝났으니 로그인·약관·닉네임을 모두 치우고 홈으로 간다.
            onSignedUp = { navigator.navigateClearingBackStack(RunVealRoute.Home.Main) },
        )
    }
}
