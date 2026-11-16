package com.swyp.runveal.sign.signup.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface TermsAgreementIntent : UiIntent {
    /** 전체 동의 — 지금 전부 체크돼 있으면 전부 해제, 아니면 전부 체크. */
    data object ToggleAll : TermsAgreementIntent
    data class Toggle(val term: RequiredTerm) : TermsAgreementIntent
}
