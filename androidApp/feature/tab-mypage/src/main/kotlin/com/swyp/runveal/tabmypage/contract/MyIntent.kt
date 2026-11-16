package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface MyIntent : UiIntent {
    data object NicknameChangeClicked : MyIntent
    data object ImportRecordsClicked : MyIntent
    /** 로그아웃 항목 탭 — 바로 실행하지 않고 확인 다이얼로그를 띄운다. */
    data object LogoutClicked : MyIntent
    data object LogoutConfirmed : MyIntent
    data object LogoutDismissed : MyIntent
    data object WithdrawClicked : MyIntent

    /** 안내 토스트가 노출을 마쳤을 때 — 다시 안 뜨게 상태에서 내린다. */
    data object ToastShown : MyIntent
}
