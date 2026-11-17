package com.sloth.runveal.sign.signup

import android.util.Log
import com.sloth.runveal.auth.AuthProvider
import com.sloth.runveal.auth.usecase.CompleteSignUpUseCase
import com.sloth.runveal.sign.signup.contract.NICKNAME_MAX_LENGTH
import com.sloth.runveal.sign.signup.contract.NicknameEffect
import com.sloth.runveal.sign.signup.contract.NicknameIntent
import com.sloth.runveal.sign.signup.contract.NicknameState
import com.sloth.runveal.sign.signup.contract.suggestedNicknameOrEmpty
import com.sloth.runveal.ui.mvi.MviViewModel

private const val TAG = "SignUp"

class NicknameViewModel : MviViewModel<NicknameIntent, NicknameState, NicknameEffect>(
    // 로그인에서 받은 소셜 계정 이름을 미리 채워 둔다 — 못 받았거나 닉네임 규칙에 안 맞으면 공란.
    initialState = NicknameState(nickname = suggestedNicknameOrEmpty(SuggestedNicknameHolder.value)),
) {
    private val completeSignUp = CompleteSignUpUseCase(AuthProvider.repository)

    override suspend fun handleIntent(intent: NicknameIntent) {
        when (intent) {
            is NicknameIntent.NicknameChanged -> setState {
                copy(nickname = intent.nickname.take(NICKNAME_MAX_LENGTH), showError = false)
            }

            NicknameIntent.CompleteClicked -> complete()

            NicknameIntent.ErrorShown -> setState { copy(showError = false) }
        }
    }

    private suspend fun complete() {
        val nickname = currentState.nickname
        if (!currentState.canSubmit) return
        setState { copy(isSubmitting = true, showError = false) }

        // TODO(서버 인증) 닉네임 중복 확인(API.md §3.8)을 여기서 먼저 호출한다.
        //  지금은 LocalAuthRemoteDataSource가 로컬에만 저장해서 중복을 알 방법이 없다.
        val result = runCatching { completeSignUp(nickname) }
        setState { copy(isSubmitting = false, showError = result.isFailure) }
        if (result.isFailure) {
            Log.w(TAG, "가입 확정 실패", result.exceptionOrNull())
            return
        }
        sendEffect(NicknameEffect.NavigateToHome)
    }
}
