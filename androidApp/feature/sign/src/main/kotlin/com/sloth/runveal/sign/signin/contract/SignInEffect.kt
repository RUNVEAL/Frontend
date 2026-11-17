package com.sloth.runveal.sign.signin.contract

import com.sloth.runveal.ui.mvi.UiEffect

sealed interface SignInEffect : UiEffect {
    /** 이미 가입을 끝낸 사용자 — 바로 홈으로 간다. */
    data object NavigateToHome : SignInEffect

    /** 신규 사용자 — 약관 동의부터 가입 절차를 밟는다. */
    data object NavigateToSignUp : SignInEffect
}
