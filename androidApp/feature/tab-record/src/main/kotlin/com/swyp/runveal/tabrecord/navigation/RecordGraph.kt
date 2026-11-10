package com.swyp.runveal.tabrecord.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabrecord.RecordScreen

fun NavGraphBuilder.recordGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Record> {
        RecordScreen()
    }
}
