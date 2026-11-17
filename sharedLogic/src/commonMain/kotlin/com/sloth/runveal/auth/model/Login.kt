package com.sloth.runveal.auth.model

/** TODO(서버 인증) 서버 계약이 확정되면 실제 응답 필드로 맞춘다.
 *
 * 로그인/회원가입 API 응답. 백엔드 계약 미정 — 필드는 docs/kmp_auth_native_design.md 2장 기준 자리표시자. */
data class Login(
    val accessToken: String?,
    val refreshToken: String?,
    val isNewUser: Boolean,
)
