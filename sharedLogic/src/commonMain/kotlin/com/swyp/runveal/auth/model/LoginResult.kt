package com.swyp.runveal.auth.model

/**
 * [com.swyp.runveal.auth.repository.AuthRepository.login] 결과. 소셜 SDK 단계만 나타내는
 * [SocialAuthResult]와 달리 서버 교환·토큰 저장까지 끝난 뒤의 상태를 담는다 — 특히 가입 절차
 * (약관 동의·닉네임)를 더 밟아야 하는 신규 사용자인지.
 */
sealed interface LoginResult {
    data class Success(val isNewUser: Boolean) : LoginResult
    data object Cancelled : LoginResult
    data class Failure(val debugMessage: String) : LoginResult
}
