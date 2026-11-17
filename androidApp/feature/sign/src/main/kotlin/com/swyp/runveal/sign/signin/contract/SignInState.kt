package com.swyp.runveal.sign.signin.contract

import com.swyp.runveal.auth.model.SocialLoginType
import com.swyp.runveal.ui.mvi.UiState

data class SignInState(
    /** 로그인 진행 중인 수단. 진행 중이 아니면 null. */
    val loadingSocial: SocialLoginType? = null,
    /** 실패 안내 노출 여부. 사용자에게 보이는 문구는 화면이 string resource에서 가져온다 —
     * SDK가 준 원인 메시지는 그대로 보여 주지 않고 로그로만 남긴다. */
    val showError: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = loadingSocial != null
}
