package com.sloth.runveal.sign.signup

import com.sloth.runveal.sign.signup.contract.RequiredTerm
import com.sloth.runveal.sign.signup.contract.TermsAgreementIntent
import com.sloth.runveal.sign.signup.contract.TermsAgreementState
import com.sloth.runveal.ui.mvi.MviViewModel

/** 동의 체크 상태만 들고 있는다 — "다음"을 눌렀을 때의 이동은 화면이 콜백으로 받는다. */
class TermsAgreementViewModel : MviViewModel<TermsAgreementIntent, TermsAgreementState, Nothing>(
    initialState = TermsAgreementState(),
) {
    override suspend fun handleIntent(intent: TermsAgreementIntent) {
        when (intent) {
            TermsAgreementIntent.ToggleAll -> setState {
                copy(agreed = if (allAgreed) emptySet() else RequiredTerm.entries.toSet())
            }

            is TermsAgreementIntent.Toggle -> setState {
                copy(agreed = if (isAgreed(intent.term)) agreed - intent.term else agreed + intent.term)
            }
        }
    }
}
