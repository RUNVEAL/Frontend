package com.sloth.runveal.sign.signin

import android.util.Log
import com.sloth.runveal.auth.AuthProvider
import com.sloth.runveal.auth.model.LoginResult
import com.sloth.runveal.auth.model.SocialLoginType
import com.sloth.runveal.auth.usecase.LoginUseCase
import com.sloth.runveal.sign.signin.contract.SignInEffect
import com.sloth.runveal.sign.signin.contract.SignInIntent
import com.sloth.runveal.sign.signin.contract.SignInState
import com.sloth.runveal.sign.signup.SuggestedNicknameHolder
import com.sloth.runveal.ui.mvi.MviViewModel

private const val TAG = "SignIn"

class SignInViewModel : MviViewModel<SignInIntent, SignInState, SignInEffect>(
    initialState = SignInState(),
) {
    private val login = LoginUseCase(AuthProvider.repository)

    override suspend fun handleIntent(intent: SignInIntent) {
        when (intent) {
            SignInIntent.KakaoLoginClicked -> signIn(SocialLoginType.KAKAO)
            SignInIntent.GoogleLoginClicked -> signIn(SocialLoginType.GOOGLE)
            SignInIntent.ErrorShown -> setState { copy(showError = false) }
        }
    }

    private suspend fun signIn(social: SocialLoginType) {
        // 카카오톡/구글 계정 선택 화면이 떠 있는 동안 다른 버튼을 눌러 SDK를 두 번 열지 않게 막는다.
        if (currentState.isLoading) return
        setState { copy(loadingSocial = social, showError = false) }

        when (val result = login(social)) {
            is LoginResult.Success -> {
                // 스플래시·홈이 보는 전역 상태를 먼저 확정한다 — 이동 후에 바꾸면 그 사이 프레임에서
                // 아직 로그아웃으로 보인다.
                AuthProvider.session.markLoggedIn()
                // 가입 화면 닉네임 기본값으로 쓸 소셜 계정 이름. 항상 덮어쓴다 — 다른 계정으로
                // 다시 로그인했을 때 옛 이름이 남으면 안 된다.
                SuggestedNicknameHolder.publish(result.suggestedNickname)
                setState { copy(loadingSocial = null) }
                sendEffect(
                    if (result.isNewUser) SignInEffect.NavigateToSignUp else SignInEffect.NavigateToHome,
                )
            }

            // 사용자가 직접 취소한 건 실패가 아니다 — 아무 안내도 띄우지 않고 로그인 화면에 머문다.
            LoginResult.Cancelled -> setState { copy(loadingSocial = null) }

            is LoginResult.Failure -> {
                Log.w(TAG, "$social 로그인 실패 — ${result.debugMessage}")
                setState { copy(loadingSocial = null, showError = true) }
            }
        }
    }
}
