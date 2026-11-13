package com.swyp.runveal.courserecommend.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface CourseRecommendResultIntent : UiIntent {
    data class SelectRoute(val routeId: String) : CourseRecommendResultIntent
}
