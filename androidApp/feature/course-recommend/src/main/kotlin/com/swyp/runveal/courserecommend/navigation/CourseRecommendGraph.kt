package com.swyp.runveal.courserecommend.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.swyp.runveal.courserecommend.CourseCreateScreen
import com.swyp.runveal.courserecommend.CourseDetailScreen
import com.swyp.runveal.courserecommend.CourseRecommendResultScreen
import com.swyp.runveal.navigation.RunVealRoute

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

fun NavGraphBuilder.courseDetailOverlay(onBack: () -> Unit) {
    composable<RunVealRoute.HomeTab.CourseDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<RunVealRoute.HomeTab.CourseDetail>()
        CourseDetailScreen(routeId = route.routeId, onBack = onBack, modifier = Modifier.fillMaxSize())
    }
}
