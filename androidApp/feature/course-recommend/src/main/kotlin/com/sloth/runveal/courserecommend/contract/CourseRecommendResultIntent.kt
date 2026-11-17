package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface CourseRecommendResultIntent : UiIntent {
    data class SelectRoute(val routeId: String) : CourseRecommendResultIntent
}
