package com.swyp.runveal.courserecommend.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.courserecommend.CourseCreateScreen
import com.swyp.runveal.navigation.RunVealRoute

fun NavGraphBuilder.courseRecommendOverlay(onBack: () -> Unit) {
    composable<RunVealRoute.HomeTab.CourseRecommend> {
        CourseCreateScreen(onBack = onBack, modifier = Modifier.fillMaxSize())
    }
}
