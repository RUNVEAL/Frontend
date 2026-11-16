package com.swyp.runveal.sign.signup.contract

import com.swyp.runveal.ui.mvi.UiEffect

sealed interface NicknameEffect : UiEffect {
    /** 가입 확정 완료 — 홈으로 넘어가고 가입 흐름 전체를 백스택에서 치운다. */
    data object NavigateToHome : NicknameEffect
}
