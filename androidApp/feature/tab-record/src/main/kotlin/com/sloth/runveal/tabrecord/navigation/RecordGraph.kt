package com.sloth.runveal.tabrecord.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.tabrecord.RecordDetailScreen
import com.sloth.runveal.tabrecord.RecordScreen

fun NavGraphBuilder.recordGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Record> {
        RecordScreen(
            onRecordClick = { trackId -> navigator.navigate(RunVealRoute.HomeTab.RecordDetail(trackId)) },
        )
    }
    composable<RunVealRoute.HomeTab.RecordDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<RunVealRoute.HomeTab.RecordDetail>()
        RecordDetailScreen(
            trackId = route.trackId,
            onBack = { navigator.popBackStack() },
        )
    }
}
