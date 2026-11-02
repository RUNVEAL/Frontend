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

    fun navigateUp(): Boolean = navController.navigateUp()

    fun popBackStack(): Boolean = navController.popBackStack()
}

@Composable
fun rememberRunVealNavigator(
    navController: NavHostController = rememberNavController(),
): RunVealNavigator = remember(navController) { RunVealNavigator(navController) }
