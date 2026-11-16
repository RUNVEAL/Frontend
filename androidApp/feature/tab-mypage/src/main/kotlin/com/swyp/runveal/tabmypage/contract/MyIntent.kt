package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface MyIntent : UiIntent {
    data object NicknameChangeClicked : MyIntent
    data object ImportRecordsClicked : MyIntent
    data object TermsClicked : MyIntent
    data object LogoutClicked : MyIntent
    data object WithdrawClicked : MyIntent
}
