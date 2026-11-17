package com.swyp.runveal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.rememberNavController

/**
 * NavHostController를 감싸 실제 이동 액션만 노출한다.
 * feature 모듈은 NavHostController를 직접 참조하지 않고 이 타입을 통해서만 이동한다.
 */
class RunVealNavigator(
    val navController: NavHostController,
) {
    inline fun <reified T : Route> navigate(route: T, navOptions: NavOptions? = null) {
        navController.navigate(route = route, navOptions = navOptions)
    }

    /**
     * 백스택을 전부 비우고 [route]로 간다. 로그인·가입을 끝내고 홈으로 갈 때처럼 뒤로 눌러 돌아갈
     * 곳을 남겨 두면 안 되는 이동에 쓴다 — 어느 화면을 거쳐 왔든(로그인만, 또는 로그인 → 약관 →
     * 닉네임) 똑같이 비워진다. 루트 그래프까지 inclusive로 걷어내는 게 "전부 비우기"의 관용구다.
     */
    inline fun <reified T : Route> navigateClearingBackStack(route: T) {
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    fun navigateUp(): Boolean = navController.navigateUp()

    fun popBackStack(): Boolean = navController.popBackStack()
}

@Composable
fun rememberRunVealNavigator(
    navController: NavHostController = rememberNavController(),
): RunVealNavigator = remember(navController) { RunVealNavigator(navController) }
