package com.sloth.runveal.courserecommend.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.sloth.runveal.courserecommend.CourseCreateScreen
import com.sloth.runveal.courserecommend.CourseDetailScreen
import com.sloth.runveal.courserecommend.CourseRecommendResultScreen
import com.sloth.runveal.courserecommend.contract.CourseDetailSource
import com.sloth.runveal.navigation.RunVealRoute

fun NavGraphBuilder.courseRecommendOverlay(onBack: () -> Unit, onNavigateToResult: () -> Unit) {
    composable<RunVealRoute.HomeTab.CourseRecommend> {
        CourseCreateScreen(onBack = onBack, onNavigateToResult = onNavigateToResult, modifier = Modifier.fillMaxSize())
    }
}

fun NavGraphBuilder.courseRecommendResultOverlay(onClose: () -> Unit, onCourseClick: (String) -> Unit) {
    composable<RunVealRoute.HomeTab.CourseRecommendResult> {
        CourseRecommendResultScreen(onClose = onClose, onCourseClick = onCourseClick, modifier = Modifier.fillMaxSize())
    }
}

fun NavGraphBuilder.courseDetailOverlay(onBack: () -> Unit, onStartRunning: () -> Unit) {
    composable<RunVealRoute.HomeTab.CourseDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<RunVealRoute.HomeTab.CourseDetail>()
        CourseDetailScreen(
            source = CourseDetailSource.Recommended(route.routeId),
            onBack = onBack,
            onStartRunning = onStartRunning,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** "내 코스" 목록에서 들어가는 코스 상세 — 추천 흐름과 같은 화면이지만 코스 탭 스택 위에 올라간다. */
fun NavGraphBuilder.savedCourseDetailOverlay(onBack: () -> Unit, onStartRunning: () -> Unit) {
    composable<RunVealRoute.HomeTab.SavedCourseDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<RunVealRoute.HomeTab.SavedCourseDetail>()
        CourseDetailScreen(
            source = CourseDetailSource.Saved(route.courseId),
            onBack = onBack,
            onStartRunning = onStartRunning,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
