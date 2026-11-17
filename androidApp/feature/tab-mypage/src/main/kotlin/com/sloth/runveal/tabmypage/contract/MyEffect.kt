package com.sloth.runveal.tabmypage.contract

import com.sloth.runveal.ui.mvi.UiEffect

sealed interface MyEffect : UiEffect {
    /** 로그아웃 완료 — 로그인 화면으로 돌아가고 백스택을 비운다. */
    data object LoggedOut : MyEffect
}
