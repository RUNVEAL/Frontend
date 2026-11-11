package com.swyp.runveal.courserecommend.contract

import com.swyp.runveal.ui.mvi.UiState

/** `RouteDto` 하나를 화면이 그대로 쓸 수 있게 포맷한 모델. */
data class RouteCardUiModel(
    val routeId: String,
    val typeLabel: String,
    val distanceLabel: String,
    val durationLabel: String,
    val elevationGainLabel: String?,
    val newRoadLabel: String?,
    val tags: List<String>,
    val milestoneNames: List<String>,
)

sealed interface CourseRecommendResultState : UiState {
    data object Loading : CourseRecommendResultState

    data class Ready(
        val routes: List<RouteCardUiModel>,
        /** 코스가 0개일 때 보여줄 안내 문구(§6.3) — 코스가 있으면 null. */
        val emptyMessage: String?,
    ) : CourseRecommendResultState
}
