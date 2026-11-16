package com.swyp.runveal.auth.usecase

import com.swyp.runveal.auth.model.LoginResult
import com.swyp.runveal.auth.model.SocialLoginType
import com.swyp.runveal.auth.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(social: SocialLoginType): LoginResult = repository.login(social)
}

/**
 * 가입 절차의 마지막 단계. 로그인([LoginUseCase])이 신규 사용자라고 알려 주면 약관 동의·닉네임을
 * 받고 여기서 가입을 확정한다 — 그때까지는 토큰만 있고 가입은 끝나지 않은 상태다.
 */
class CompleteSignUpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(nickname: String) = repository.completeSignUp(nickname)
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
