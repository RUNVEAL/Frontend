package com.sloth.runveal.tabcourse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.result.SelectedCourseHolder
import com.sloth.runveal.tabcourse.CourseScreen

fun NavGraphBuilder.courseGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Course> {
        CourseScreen(
            onCreateCourseClick = { navigator.navigate(RunVealRoute.HomeTab.CourseRecommend) },
            // 목록에서 한 건을 누르면 코스 상세로 — 값은 id만 넘기고 화면이 로컬 DB에서 다시 읽는다.
            onCourseClick = { course -> navigator.navigate(RunVealRoute.HomeTab.SavedCourseDetail(course.id)) },
            // 코스 상세의 "이 코스로 러닝 시작"과 같은 패턴 — 기록 화면이 점선 안내선과 출발·도착
            // 핀을 그리는 데 쓴다. 순환 코스는 도착 핀을 생략해야 해서 isLoop도 같이 넘긴다.
            onStartCourse = { course ->
                SelectedCourseHolder.publish(course.path, course.isLoop)
                navigator.navigate(RunVealRoute.HomeTab.Recording)
            },
        )
    }
}
