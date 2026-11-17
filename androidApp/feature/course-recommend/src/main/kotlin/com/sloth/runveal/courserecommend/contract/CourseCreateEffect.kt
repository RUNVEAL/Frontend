package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.ui.mvi.UiEffect

sealed interface CourseCreateEffect : UiEffect {
    /** `/api/v1/routes` 응답을 받은 뒤(성공/실패 모두) 결과 화면으로 넘어간다. */
    data object NavigateToResult : CourseCreateEffect
}
