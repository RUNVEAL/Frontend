package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.courserecommend.CourseRecommendSettings
import com.sloth.runveal.ui.mvi.UiIntent

sealed interface CourseRecommendResultIntent : UiIntent {
    data class SelectRoute(val routeId: String) : CourseRecommendResultIntent

    /** 상단 필터 칩 — 코스 조건 설정 시트를 지금 조건 그대로 띄운다. */
    data object OpenFilterSheet : CourseRecommendResultIntent

    /** 시트를 닫는다(뒤로가기·바깥 탭 포함) — 고치던 값은 버린다. */
    data object CloseFilterSheet : CourseRecommendResultIntent

    /** 시트에서 값을 하나 바꿨다. 적용 전까지는 초안에만 쌓인다. */
    data class UpdateFilterDraft(val draft: CourseRecommendSettings) : CourseRecommendResultIntent

    /** 시트의 "초기화" — 코스 만들기와 같은 기본값으로 되돌린다(출발·도착지는 유지). */
    data object ResetFilterDraft : CourseRecommendResultIntent

    /** "코스 조건 적용하기" — 바꾼 조건으로 다시 추천받는다. */
    data object ApplyFilterDraft : CourseRecommendResultIntent
}
