package com.swyp.runveal.tabhome.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabhome.HomeMapOverlayScreen

fun NavGraphBuilder.homeMapOverlay(onCreateCourseClick: () -> Unit = {}) {
    composable<RunVealRoute.HomeTab.HomeMap> {
        HomeMapOverlayScreen(modifier = Modifier.fillMaxSize(), onCreateCourseClick = onCreateCourseClick)
    }
}
