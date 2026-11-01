package com.swyp.runveal.auth.usecase

import com.swyp.runveal.auth.model.SocialAuthResult
import com.swyp.runveal.auth.model.SocialLoginType
import com.swyp.runveal.auth.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(social: SocialLoginType): SocialAuthResult = repository.login(social)
}

/**
 * 백엔드 회원가입 계약(추가 입력 필드 등)이 아직 미정이라, 지금은 [LoginUseCase]와 동일하게
 * 소셜 로그인 결과를 그대로 교환한다. 가입 시 추가 정보 입력이 필요해지면 그때 분리한다.
 */
class SignUpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(social: SocialLoginType): SocialAuthResult = repository.login(social)
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
