package com.swyp.runveal.tabcourse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.result.SelectedCourseHolder
import com.swyp.runveal.tabcourse.CourseScreen

fun NavGraphBuilder.courseGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Course> {
        CourseScreen(
            onCreateCourseClick = { navigator.navigate(RunVealRoute.HomeTab.CourseRecommend) },
            // 코스 상세의 "이 코스로 러닝 시작"과 같은 패턴 — 경로만 넘기고 가이드/오버레이 적용은 보류.
            onStartCourse = { path ->
                SelectedCourseHolder.publish(path)
                navigator.navigate(RunVealRoute.HomeTab.Recording)
            },
        )
    }
}
