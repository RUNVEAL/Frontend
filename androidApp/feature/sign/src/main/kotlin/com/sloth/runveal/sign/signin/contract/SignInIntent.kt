package com.sloth.runveal.sign.signin.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface SignInIntent : UiIntent {
    data object KakaoLoginClicked : SignInIntent
    data object GoogleLoginClicked : SignInIntent

    /** 실패 토스트가 노출을 마쳤을 때 — 다시 안 뜨게 상태에서 내린다. */
    data object ErrorShown : SignInIntent
}
