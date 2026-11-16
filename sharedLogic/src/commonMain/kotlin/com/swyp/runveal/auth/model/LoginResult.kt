package com.swyp.runveal.auth.model

/**
 * [com.swyp.runveal.auth.repository.AuthRepository.login] 결과. 소셜 SDK 단계만 나타내는
 * [SocialAuthResult]와 달리 서버 교환·토큰 저장까지 끝난 뒤의 상태를 담는다 — 특히 가입 절차
 * (약관 동의·닉네임)를 더 밟아야 하는 신규 사용자인지.
 */
sealed interface LoginResult {
    /**
     * [suggestedNickname]은 가입 화면 닉네임 입력의 기본값으로 쓸 소셜 계정 이름. 지금은 소셜 SDK가
     * 준 값을 그대로 올려 보내고, 서버 인증 API가 붙으면 로그인 응답이 주는 값으로 바뀐다. 못 받았으면 null.
     * TODO(서버 인증) 출처를 소셜 SDK → 로그인 응답으로 바꾼다.
     */
    data class Success(val isNewUser: Boolean, val suggestedNickname: String? = null) : LoginResult
    data object Cancelled : LoginResult
    data class Failure(val debugMessage: String) : LoginResult
}
