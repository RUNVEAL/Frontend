package com.swyp.runveal.tabcourse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabcourse.CourseScreen

fun NavGraphBuilder.courseGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Course> {
        CourseScreen()
    }
}
