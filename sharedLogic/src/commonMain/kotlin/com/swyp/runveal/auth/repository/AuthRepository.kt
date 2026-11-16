package com.swyp.runveal.auth.repository

import com.swyp.runveal.auth.model.LoginResult
import com.swyp.runveal.auth.model.SocialLoginType

interface AuthRepository {
    /** 소셜 로그인 1회 전체 플로우: SDK 로그인 → 서버 교환 → 토큰 저장. */
    suspend fun login(social: SocialLoginType): LoginResult

    suspend fun logout()

    /** 약관 동의·닉네임까지 받아 회원가입을 마무리한다. */
    suspend fun completeSignUp(nickname: String)

    /** 가입 절차를 이미 끝냈는지. 토큰은 있는데 이게 false면 가입 도중에 앱이 끊긴 상태다. */
    suspend fun isSignUpCompleted(): Boolean

    /** 로컬에 저장된 AccessToken을 그대로 읽기만 한다 (네트워크 호출 없음). */
    suspend fun getAccessToken(): String?

    /**
     * 401 복구/자동 로그인용 세션 복구. 동시에 여러 번 불려도 실제 복구는 한 번만 수행한다.
     * 복구 성공 시 새 AccessToken, 실패(진짜 로그아웃 상태) 시 null.
     */
    suspend fun recoverSession(): String?
}
