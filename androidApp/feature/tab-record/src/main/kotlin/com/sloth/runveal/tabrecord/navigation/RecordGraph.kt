package com.sloth.runveal.tabrecord.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.tabrecord.RecordDetailScreen
import com.sloth.runveal.tabrecord.RecordScreen

/** [onStartRunningClick]은 기록이 없을 때 뜨는 "러닝 시작하기" — 홈 탭 줌인 뷰로 보내는 일이라
 * 탭을 쥐고 있는 쪽(HomeScreen)이 내려준다. */
fun NavGraphBuilder.recordGraph(
    navigator: RunVealNavigator,
    onStartRunningClick: () -> Unit,
) {
    composable<RunVealRoute.HomeTab.Record> {
        RecordScreen(
            onRecordClick = { trackId -> navigator.navigate(RunVealRoute.HomeTab.RecordDetail(trackId)) },
            onStartRunningClick = onStartRunningClick,
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
