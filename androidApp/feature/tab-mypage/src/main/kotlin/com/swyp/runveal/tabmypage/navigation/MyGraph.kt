package com.swyp.runveal.tabmypage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabmypage.MyScreen

fun NavGraphBuilder.myGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.My> {
        MyScreen()
    }
}
