package com.sloth.runveal.auth.usecase

import com.sloth.runveal.auth.model.LoginResult
import com.sloth.runveal.auth.model.SocialLoginType
import com.sloth.runveal.auth.repository.AuthRepository

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

/** 마이페이지 등에서 지금 로그인한 사람의 닉네임을 읽는다. 가입 전이면 null. */
class GetNicknameUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): String? = repository.nickname()
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
