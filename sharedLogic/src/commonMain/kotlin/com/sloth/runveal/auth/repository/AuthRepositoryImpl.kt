package com.sloth.runveal.auth.repository

import com.sloth.runveal.auth.model.LoginResult
import com.sloth.runveal.auth.model.SocialAuthResult
import com.sloth.runveal.auth.model.SocialLoginType
import com.sloth.runveal.auth.service.GoogleAuthService
import com.sloth.runveal.auth.service.KakaoAuthService
import com.sloth.runveal.auth.service.SocialAuthService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * docs/kmp_auth_native_design.md 2-2 "세션 복구 로직"을 그대로 포팅.
 * [recoverSession]은 동시에 여러 번 호출돼도(예: 여러 API가 동시에 401을 맞는 경우) 단일
 * 비행(single-flight) 잠금으로 실제 복구는 한 번만 수행한다.
 */
class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val kakao: KakaoAuthService,
    private val google: GoogleAuthService,
) : AuthRepository {

    private val recoverMutex = Mutex()
    private var recovering: Deferred<String?>? = null

    override suspend fun login(social: SocialLoginType): LoginResult {
        val signIn = when (val signInResult = serviceFor(social).signIn()) {
            is SocialAuthResult.Success -> signInResult
            SocialAuthResult.Cancelled -> return LoginResult.Cancelled
            is SocialAuthResult.Failure -> return LoginResult.Failure(signInResult.debugMessage)
        }
        val socialToken = signIn.token

        val login = runCatching { remote.login(socialToken, social) }
            .getOrElse { return LoginResult.Failure(it.message ?: "로그인 요청 실패") }
        val accessToken = login.accessToken
            ?: return LoginResult.Failure("서버가 AccessToken을 내려주지 않음")

        remote.saveTokens(accessToken, login.refreshToken)
        return LoginResult.Success(isNewUser = login.isNewUser, suggestedNickname = signIn.displayName)
    }

    override suspend fun completeSignUp(nickname: String) = remote.completeSignUp(nickname)

    override suspend fun isSignUpCompleted(): Boolean = remote.isSignUpCompleted()

    override suspend fun nickname(): String? = remote.nickname()

    override suspend fun logout() {
        val social = runCatching { remote.getSocialLoginType() }.getOrNull()
        social?.let { runCatching { serviceFor(it).signOut() } }
        runCatching { remote.logout() }
    }

    override suspend fun getAccessToken(): String? = remote.getAccessToken()

    override suspend fun recoverSession(): String? = coroutineScope {
        recoverMutex.withLock {
            recovering?.let { return@withLock it.await() }
            val deferred = async { runRecover() }
            recovering = deferred
            try {
                deferred.await()
            } finally {
                recovering = null
            }
        }
    }

    private suspend fun runRecover(): String? {
        remote.reissueAccessToken()?.let { return it }

        val social = remote.getSocialLoginType() ?: return null
        val socialToken = silentSocialToken(social) ?: return null
        val login = runCatching { remote.login(socialToken, social) }.getOrNull() ?: return null
        if (login.isNewUser || login.accessToken == null) return null

        remote.saveTokens(login.accessToken, login.refreshToken)
        return login.accessToken
    }

    private suspend fun silentSocialToken(social: SocialLoginType): String? =
        serviceFor(social).silentToken()

    private fun serviceFor(social: SocialLoginType): SocialAuthService = when (social) {
        SocialLoginType.KAKAO -> kakao
        SocialLoginType.GOOGLE -> google
        // Apple 로그인은 iOS 전용이라 AuthRepository에 연결돼 있지 않다 — iOS 쪾은
        // AppleAuthService.signIn()을 직접 호출해 토큰을 받은 뒤 백엔드 연동부(AuthRemoteDataSource)
        // 확정 시 별도 경로로 교환해야 한다.
        SocialLoginType.APPLE -> error("Apple 로그인은 AuthRepository로 처리하지 않는다 (iOS 전용, 별도 경로 필요)")
    }
}
