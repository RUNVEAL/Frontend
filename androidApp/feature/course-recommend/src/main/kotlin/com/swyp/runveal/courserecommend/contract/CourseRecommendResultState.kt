package com.swyp.runveal.courserecommend.contract

import com.swyp.runveal.courserecommend.CourseRecommendSettings
import com.swyp.runveal.ui.mvi.UiState

/** `RouteDto` 하나를 [RunvealCourseCard][com.swyp.runveal.designsystem.component.list.RunvealCourseCard]가
 * 그대로 쓸 수 있게 포맷한 모델. */
data class RouteCardUiModel(
    val routeId: String,
    /** "코스 A", "코스 B" ... 라벨의 글자 부분. 전체 문구는 화면에서 string resource로 조합한다. */
    val courseLabel: String,
    val tags: List<String>,
    val distanceValue: String,
    val distanceUnit: String,
    val timeValue: String,
    val timeUnit: String,
    val elevationValue: String,
    val elevationUnit: String,
    val newRoadValue: String,
    val newRoadUnit: String,
)

sealed interface CourseRecommendResultState : UiState {
    data object Loading : CourseRecommendResultState

    data class Ready(
        val routes: List<RouteCardUiModel>,
        /** 코스가 0개일 때 보여줄 안내 문구(§6.3) — 코스가 있으면 null. */
        val emptyMessage: String?,
        /** 지도 위 코스 리스트에서 선택된 카드. 코스가 있으면 첫 코스가 기본 선택. */
        val selectedRouteId: String? = null,
        /** 이 결과를 만든 B0 설정값 — 앱바 아래 칩으로 그대로 보여준다. */
        val settings: CourseRecommendSettings,
    ) : CourseRecommendResultState
}
