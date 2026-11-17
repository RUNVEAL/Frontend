package com.sloth.runveal.tabrecord.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.tabrecord.RecordScreen

fun NavGraphBuilder.recordGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Record> {
        RecordScreen()
    }
}
