package com.sloth.runveal.tabcourse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sloth.runveal.analytics.RunvealAnalyticsLogger
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.result.SelectedCourseHolder
import com.sloth.runveal.tabcourse.CourseScreen

fun NavGraphBuilder.courseGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Course> {
        CourseScreen(
            onCreateCourseClick = { navigator.navigate(RunVealRoute.HomeTab.CourseRecommend) },
            // 줄 오른쪽 아이콘을 누르면 코스 상세로 — 값은 id만 넘기고 화면이 로컬 DB에서 다시 읽는다.
            // 줄 전체를 누르는 건 위 큰 카드를 바꾸는 동작이라 화면 안에서 끝난다(네비게이션 없음).
            onCourseDetailClick = { course -> navigator.navigate(RunVealRoute.HomeTab.SavedCourseDetail(course.id)) },
            // 코스 상세의 "이 코스로 러닝 시작"과 같은 패턴 — 기록 화면이 점선 안내선과 출발·도착
            // 핀을 그리는 데 쓴다. 순환 코스는 도착 핀을 생략해야 해서 isLoop도 같이 넘긴다.
            onStartCourse = { course ->
                RunvealAnalyticsLogger.logCourseStart(fromRecommend = false)
                SelectedCourseHolder.publish(course.path, course.isLoop, courseId = course.id)
                navigator.navigate(RunVealRoute.HomeTab.Recording)
            },
        )
    }
}
