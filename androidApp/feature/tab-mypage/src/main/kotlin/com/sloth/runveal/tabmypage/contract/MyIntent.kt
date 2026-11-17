package com.sloth.runveal.tabmypage.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface MyIntent : UiIntent {
    data object NicknameChangeClicked : MyIntent
    /** 로그아웃 항목 탭 — 바로 실행하지 않고 확인 다이얼로그를 띄운다. */
    data object LogoutClicked : MyIntent
    data object LogoutConfirmed : MyIntent
    data object LogoutDismissed : MyIntent
    data object WithdrawClicked : MyIntent

    /** 화면이 직접 다루는 일(권한 등)의 결과를 안내 토스트로 띄울 때. */
    data class ToastRequested(val toast: MyToast) : MyIntent

    /** 안내 토스트가 노출을 마쳤을 때 — 다시 안 뜨게 상태에서 내린다. */
    data object ToastShown : MyIntent
}
