package com.swyp.runveal.auth.model

/** 로그인/회원가입 API 응답. 백엔드 계약 미정 — 필드는 docs/kmp_auth_native_design.md 2장 기준 자리표시자. */
data class Login(
    val accessToken: String?,
    val refreshToken: String?,
    val isNewUser: Boolean,
)
