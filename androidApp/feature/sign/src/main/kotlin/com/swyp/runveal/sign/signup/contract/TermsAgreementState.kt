package com.swyp.runveal.sign.signup.contract

import com.swyp.runveal.ui.mvi.UiState

/** 가입에 필요한 필수 약관. 전부 동의해야 다음으로 넘어갈 수 있다(선택 약관은 아직 없다). */
enum class RequiredTerm { Service, Privacy, Location }

data class TermsAgreementState(
    val agreed: Set<RequiredTerm> = emptySet(),
) : UiState {
    val allAgreed: Boolean get() = agreed.size == RequiredTerm.entries.size

    fun isAgreed(term: RequiredTerm): Boolean = term in agreed
}
