package com.swyp.runveal.auth.repository

import com.swyp.runveal.auth.model.Login
import com.swyp.runveal.auth.model.SocialLoginType

/**
 * 백엔드 API 호출 + 토큰 로컬 저장 경계 (Flutter 원본의 `AuthRepositoryImpl`을 KMP에서는
 * remote/local을 합쳐 단순화). **백엔드 계약이 아직 미정이라 구현을 두지 않는다** —
 * docs/kmp_auth_native_design.md 참고. 백엔드가 확정되면 Ktor `HttpClient` 기반 구현을
 * androidMain/iosMain 없이 commonMain에 바로 추가한다 (Ktor는 멀티플랫폼이라 actual이 필요 없음).
 */
interface AuthRemoteDataSource {
    /** 소셜 토큰을 서버에 보내 로그인/회원가입을 수행. */
    suspend fun login(socialToken: String, social: SocialLoginType): Login

    /** RefreshToken으로 AccessToken만 재발급하고 저장까지 책임진다. 실패/RefreshToken 없음 시 null. */
    suspend fun reissueAccessToken(): String?

    /** 마지막으로 로그인했던 소셜 종류. 무중단 재인증 시 어떤 SDK로 silent 로그인할지 판단용. */
    suspend fun getSocialLoginType(): SocialLoginType?

    suspend fun saveTokens(accessToken: String, refreshToken: String?)

    /** 로컬에 저장된 AccessToken을 그대로 읽기만 한다 (네트워크 호출 없음). */
    suspend fun getAccessToken(): String?

    /** 약관 동의·닉네임까지 받아 회원가입을 마무리한다 (인증/회원 API의 "회원가입 완료", API.md §3.8). */
    suspend fun completeSignUp(nickname: String)

    /** 가입 절차를 이미 끝냈는지. 토큰은 있는데 이게 false면 가입 도중에 앱이 끊긴 상태다. */
    suspend fun isSignUpCompleted(): Boolean

    /**
     * 가입 때 정한 닉네임. 아직 가입을 안 끝냈으면 null.
     *
     * TODO(서버 인증) 지금은 [completeSignUp]이 로컬에 적어 둔 값을 그대로 읽는다. 서버가 붙으면 회원 조회
     * API가 따로 없으므로(API.md §3.8의 7개 목록에 없음) 로그인 응답이 준 닉네임을 캐시해 읽는
     * 형태가 된다 — 읽는 쪽 코드는 그대로 두고 여기 구현만 바꾸면 된다.
     */
    suspend fun nickname(): String?

    suspend fun logout()
}
