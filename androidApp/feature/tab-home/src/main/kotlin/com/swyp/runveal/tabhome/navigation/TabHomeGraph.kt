package com.swyp.runveal.tabhome.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute

fun NavGraphBuilder.tabHomeGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Home> {
        Box(modifier = Modifier.fillMaxSize())
    }
}
