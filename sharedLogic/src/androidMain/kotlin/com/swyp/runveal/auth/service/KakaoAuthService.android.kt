package com.swyp.runveal.auth.service

import com.kakao.sdk.auth.TokenManagerProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.swyp.runveal.auth.AndroidActivityHolder
import com.swyp.runveal.auth.model.SocialAuthResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class KakaoAuthService actual constructor() : SocialAuthService {
    actual override suspend fun signIn(): SocialAuthResult = suspendCancellableCoroutine { cont ->
        fun resumeWith(token: OAuthToken?, error: Throwable?) {
            if (cont.isCompleted) return
            when {
                token != null -> cont.resume(SocialAuthResult.Success(token.accessToken))
                isUserCancelled(error) -> cont.resume(SocialAuthResult.Cancelled)
                else -> cont.resume(SocialAuthResult.Failure(error?.message ?: "카카오 로그인 실패"))
            }
        }

        val activity = AndroidActivityHolder.current
        if (activity == null) {
            cont.resume(SocialAuthResult.Failure("foreground Activity 없음 — AndroidActivityHolder 미등록"))
            return@suspendCancellableCoroutine
        }

        // loginWithKakaoTalk은 카카오톡 앱으로 startActivity()를 하기 때문에 반드시 Activity
        // 컨텍스트여야 한다 — Application 컨텍스트로 넘기면 FLAG_ACTIVITY_NEW_TASK 예외가 나서
        // (catch되어) 매번 계정 로그인으로 폴백되는 버그가 있었다.
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                when {
                    token != null -> resumeWith(token, null)
                    isUserCancelled(error) -> resumeWith(null, error)
                    // 카카오톡 로그인 자체가 실패(미설치/오류)한 경우만 계정 로그인으로 폴백.
                    else -> UserApiClient.instance.loginWithKakaoAccount(activity) { t, e -> resumeWith(t, e) }
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(activity) { token, error -> resumeWith(token, error) }
        }
    }

    /** 카카오 SDK가 내부에 들고 있는 토큰이 유효한지(필요하면 자동 갱신까지) 확인한다. */
    actual override suspend fun silentToken(): String? = suspendCancellableCoroutine { cont ->
        UserApiClient.instance.accessTokenInfo { _, error ->
            val token = if (error == null) {
                TokenManagerProvider.instance.manager.getToken()?.accessToken
            } else {
                null
            }
            if (!cont.isCompleted) cont.resume(token)
        }
    }

    actual override suspend fun signOut() {
        suspendCancellableCoroutine<Unit> { cont ->
            UserApiClient.instance.logout { if (!cont.isCompleted) cont.resume(Unit) }
        }
    }

    private fun isUserCancelled(error: Throwable?): Boolean =
        error is ClientError && error.reason == ClientErrorCause.Cancelled
}
