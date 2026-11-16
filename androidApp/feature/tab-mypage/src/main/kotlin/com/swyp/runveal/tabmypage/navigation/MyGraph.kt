package com.swyp.runveal.tabmypage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabmypage.MyScreen

/**
 * @param onLoggedOut 로그아웃 후 이동. 로그인 화면은 탭 바깥(루트 그래프)에 있어서 여기 [navigator]
 *  (탭 네비게이터)로는 못 간다 — 루트 네비게이터를 쥔 `homeGraph`가 넘겨준다.
 */
fun NavGraphBuilder.myGraph(navigator: RunVealNavigator, onLoggedOut: () -> Unit) {
    composable<RunVealRoute.HomeTab.My> {
        MyScreen(onLoggedOut = onLoggedOut)
    }
}
