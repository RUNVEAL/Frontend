package com.swyp.runveal.sign.signup

import android.util.Log
import com.swyp.runveal.auth.AuthProvider
import com.swyp.runveal.auth.usecase.CompleteSignUpUseCase
import com.swyp.runveal.sign.signup.contract.NICKNAME_MAX_LENGTH
import com.swyp.runveal.sign.signup.contract.NicknameEffect
import com.swyp.runveal.sign.signup.contract.NicknameIntent
import com.swyp.runveal.sign.signup.contract.NicknameState
import com.swyp.runveal.ui.mvi.MviViewModel

private const val TAG = "SignUp"

class NicknameViewModel : MviViewModel<NicknameIntent, NicknameState, NicknameEffect>(
    initialState = NicknameState(),
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

        // TODO: 닉네임 중복 확인(API.md §3.8)은 서버 인증 API가 붙은 뒤에 여기서 먼저 호출한다.
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
