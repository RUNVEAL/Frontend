package com.sloth.runveal.onboarding.contract

import com.sloth.runveal.ui.mvi.UiEffect
import com.sloth.runveal.ui.mvi.UiIntent
import com.sloth.runveal.ui.mvi.UiState

sealed interface OnboardingIntent : UiIntent {
    /** 마지막 장에서 "다음"을 눌렀거나, 아무 장에서 "건너뛰기"를 눌렀다 — 둘 다 온보딩을 끝낸다. */
    data object Finished : OnboardingIntent
}

/** 온보딩은 보여주기만 하는 화면이라 ViewModel이 들고 있을 상태가 없다 — 지금 몇 번째 장인지는
 * 스와이프와 함께 움직이는 값이라 화면의 페이저(`rememberPagerState`)가 들고 있다. */
data object OnboardingState : UiState

sealed interface OnboardingEffect : UiEffect {
    data object NavigateToHome : OnboardingEffect
}
