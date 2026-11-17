package com.swyp.runveal.auth.repository

import com.swyp.runveal.auth.model.Login
import com.swyp.runveal.auth.model.SocialLoginType
import com.swyp.runveal.auth.storage.SecureTokenStore
import com.swyp.runveal.auth.storage.StorageKey

/**
 * TODO(서버 인증) **이 파일은 서버 인증 API가 붙으면 통째로 지운다.** 같은 태그가 붙은 곳을 전부 찾아
 * (`grep -rn "TODO(서버 인증)"`) 함께 정리해야 한다 — 로컬 저장으로 흉내 낸 세션/가입 상태가 여기저기 걸쳐 있다.
 *
 * 서버 인증 API가 아직 없어서 쓰는 임시 [AuthRemoteDataSource]. 소셜 SDK가 준 토큰을 그대로 세션
 * 토큰으로 보고 [SecureTokenStore]에만 넣는다 — 서버 교환·재발급이 없으니 "로그인 여부"는 곧
 * "이 기기에 소셜 로그인 기록이 남아 있는지"다.
 *
 * `docs/api/API.md` §3.8이 가리키는 인증 API 설계 문서(`docs/kmp_auth_native_design.md`)가 저장소에
 * 없어서 엔드포인트·요청/응답 형태를 아직 맞출 수 없다. 계약이 확정되면 Ktor 기반
 * `RemoteAuthRemoteDataSource`를 commonMain에 새로 만들고 [com.swyp.runveal.auth.AuthProvider]가
 * 조립하는 구현만 갈아 끼우면 된다 — 위쪽(AuthRepositoryImpl·화면)은 손대지 않아도 된다.
 *
 * 그래서 지금은 이런 제약이 있다.
 *  - [reissueAccessToken]은 항상 null이다(재발급해 줄 서버가 없다). 세션 복구는 소셜 SDK의
 *    무중단 재인증([AuthRepositoryImpl.recoverSession])에만 의존한다.
 *  - [Login.isNewUser]는 계정이 아니라 **기기** 기준이다([StorageKey.SIGN_UP_COMPLETED]) — 한 기기에서
 *    가입을 끝낸 뒤 로그아웃하고 다른 계정으로 로그인하면 약관·닉네임을 다시 받지 않는다.
 *  - [completeSignUp]이 받은 닉네임은 로컬에만 남는다(중복 확인도 못 한다).
 */
class LocalAuthRemoteDataSource(
    private val store: SecureTokenStore = SecureTokenStore(),
) : AuthRemoteDataSource {

    override suspend fun login(socialToken: String, social: SocialLoginType): Login {
        // 로그아웃·무중단 재인증 때 어느 SDK를 쓸지 알아야 하므로 로그인 수단을 같이 남긴다.
        store.write(StorageKey.SOCIAL_LOGIN_TYPE, social.name)
        return Login(
            accessToken = socialToken,
            refreshToken = null,
            // 서버가 신규 여부를 알려 줄 수 없으니 "이 기기에서 가입 절차를 끝냈는지"로 대신 판단한다.
            isNewUser = !isSignUpCompleted(),
        )
    }

    override suspend fun completeSignUp(nickname: String) {
        store.write(StorageKey.NICKNAME, nickname)
        store.write(StorageKey.SIGN_UP_COMPLETED, true.toString())
    }

    override suspend fun isSignUpCompleted(): Boolean =
        store.read(StorageKey.SIGN_UP_COMPLETED).toBoolean()

    override suspend fun nickname(): String? = store.read(StorageKey.NICKNAME)

    override suspend fun reissueAccessToken(): String? = null

    override suspend fun getSocialLoginType(): SocialLoginType? =
        store.read(StorageKey.SOCIAL_LOGIN_TYPE)
            ?.let { saved -> SocialLoginType.entries.firstOrNull { it.name == saved } }

    override suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        store.write(StorageKey.ACCESS_TOKEN, accessToken)
        if (refreshToken != null) {
            store.write(StorageKey.REFRESH_TOKEN, refreshToken)
        } else {
            store.delete(StorageKey.REFRESH_TOKEN)
        }
    }

    override suspend fun getAccessToken(): String? = store.read(StorageKey.ACCESS_TOKEN)

    override suspend fun logout() {
        store.delete(StorageKey.ACCESS_TOKEN)
        store.delete(StorageKey.REFRESH_TOKEN)
        store.delete(StorageKey.SOCIAL_LOGIN_TYPE)
        // 가입 완료 표시·닉네임은 남겨 둔다 — 로그아웃은 탈퇴가 아니라서, 다시 로그인하는 사람에게
        // 약관 동의와 닉네임을 또 받을 이유가 없다. 회원 탈퇴 API가 붙으면 그쪽에서 같이 지운다.
    }
}
