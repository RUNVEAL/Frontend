package com.sloth.runveal.auth.service

import com.sloth.runveal.auth.model.SocialAuthResult

/**
 * 소셜 로그인 SDK를 플랫폼 무관하게 다루는 공통 인터페이스. Kakao/Google은 Android·iOS 둘 다
 * expect/actual로 제공한다. Apple 로그인은 iOS 전용이라 이 인터페이스를 iosMain에서만
 * 구현한다 (AppleAuthService.ios.kt, expect/actual 아님).
 */
interface SocialAuthService {
    suspend fun signIn(): SocialAuthResult

    /** UI 없이 세션을 복원해 토큰을 재획득. 실패 시 null. (무중단 재인증용) */
    suspend fun silentToken(): String?

    suspend fun signOut()
}

expect class KakaoAuthService() : SocialAuthService {
    override suspend fun signIn(): SocialAuthResult
    override suspend fun silentToken(): String?
    override suspend fun signOut()
}

expect class GoogleAuthService() : SocialAuthService {
    override suspend fun signIn(): SocialAuthResult
    override suspend fun silentToken(): String?
    override suspend fun signOut()
}

// AppleAuthService는 expect/actual이 아니다 — Apple 로그인은 iOS 전용 기능이라
// iosMain에만 평범한 Kotlin 클래스로 있다 (sharedLogic/src/iosMain/.../AppleAuthService.ios.kt).
// Android는 Apple 로그인을 제공하지 않는다.
