package com.swyp.runveal.courserecommend.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface CourseDetailIntent : UiIntent {
    /** 화면이 뜰 때 한 번 — [CourseRecommendResultHolder][com.swyp.runveal.courserecommend.CourseRecommendResultHolder]에서
     * 이 id로 코스를 다시 찾아온다(네비게이션 인자로 전체 응답을 못 넘겨서). */
    data class Load(val routeId: String) : CourseDetailIntent
}
