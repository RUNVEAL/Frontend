package com.swyp.runveal.home.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.home.CourseScreen
import com.swyp.runveal.home.MyScreen
import com.swyp.runveal.home.RecordScreen
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute

fun NavGraphBuilder.homeTabGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Home> {
        Box(modifier = Modifier.fillMaxSize())
    }
    composable<RunVealRoute.HomeTab.Course> {
        CourseScreen()
    }
    composable<RunVealRoute.HomeTab.Record> {
        RecordScreen()
    }
    composable<RunVealRoute.HomeTab.My> {
        MyScreen()
    }
}
