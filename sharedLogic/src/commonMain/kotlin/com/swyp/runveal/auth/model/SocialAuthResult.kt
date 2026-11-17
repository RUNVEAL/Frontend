package com.swyp.runveal.auth.model

/** 소셜 SDK의 OAuth 결과. 세 소셜 로그인(카카오/구글/애플) 모두 이 타입 하나로 통일한다. */
sealed interface SocialAuthResult {
    /**
     * [displayName]은 소셜 계정에 설정된 닉네임·이름. 가입 화면의 닉네임 기본값으로만 쓰는
     * 부가 정보라 못 받아도(동의 항목 거부, 조회 실패) 로그인은 그대로 성공시킨다 — 그때는 null.
     */
    data class Success(val token: String, val displayName: String? = null) : SocialAuthResult
    data object Cancelled : SocialAuthResult
    data class Failure(val debugMessage: String) : SocialAuthResult
}
