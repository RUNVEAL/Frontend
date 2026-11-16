package com.swyp.runveal.sign.signin.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.sign.signin.SignInScreen

fun NavGraphBuilder.signInGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Sign.SignIn> {
        SignInScreen(
            // 로그인 화면은 백스택에서 치운다 — 홈에서 뒤로 눌러 다시 돌아오면 안 된다.
            onSignedIn = { navigator.navigateClearingBackStack(RunVealRoute.Home.Main) },
            // 가입 절차는 로그인 화면을 남겨 둔 채 쌓는다 — 약관에서 뒤로 누르면 로그인으로 돌아가
            // 다른 계정으로 다시 시작할 수 있다.
            onNeedsSignUp = { navigator.navigate(RunVealRoute.Sign.Terms) },
        )
    }
}
