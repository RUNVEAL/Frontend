package com.swyp.runveal.sign.signup.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface NicknameIntent : UiIntent {
    data class NicknameChanged(val nickname: String) : NicknameIntent
    data object CompleteClicked : NicknameIntent

    /** 실패 토스트가 노출을 마쳤을 때 — 다시 안 뜨게 상태에서 내린다. */
    data object ErrorShown : NicknameIntent
}
