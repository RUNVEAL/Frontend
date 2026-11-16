package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface MyIntent : UiIntent {
    data object NicknameChangeClicked : MyIntent
    data object ImportRecordsClicked : MyIntent
    data object TermsClicked : MyIntent
    data object LogoutClicked : MyIntent
    data object WithdrawClicked : MyIntent

    /** 안내 토스트가 노출을 마쳤을 때 — 다시 안 뜨게 상태에서 내린다. */
    data object ToastShown : MyIntent
}
