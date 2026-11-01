package com.swyp.runveal.auth.model

/** 소셜 SDK의 OAuth 결과. 세 소셜 로그인(카카오/구글/애플) 모두 이 타입 하나로 통일한다. */
sealed interface SocialAuthResult {
    data class Success(val token: String) : SocialAuthResult
    data object Cancelled : SocialAuthResult
    data class Failure(val debugMessage: String) : SocialAuthResult
}
