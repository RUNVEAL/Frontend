package com.swyp.runveal.auth

import com.swyp.runveal.auth.repository.AuthRepository
import com.swyp.runveal.auth.repository.AuthRepositoryImpl
import com.swyp.runveal.auth.repository.LocalAuthRemoteDataSource
import com.swyp.runveal.auth.service.GoogleAuthService
import com.swyp.runveal.auth.service.KakaoAuthService
import com.swyp.runveal.auth.state.AuthViewModel

/**
 * 앱 전체가 공유하는 인증 객체 하나씩. DI 프레임워크를 안 쓰는 프로젝트라
 * [com.swyp.runveal.data.local.RunvealDatabaseProvider]처럼 `by lazy` 싱글톤으로 둔다.
 *
 * 화면마다 [AuthRepositoryImpl]을 새로 만들면 안 된다 — `recoverSession()`의 단일 비행 잠금이
 * 인스턴스 안에 있어서, 인스턴스가 여러 개면 같은 복구가 동시에 여러 번 돌아간다. [session]도
 * 하나여야 한다: 스플래시가 판정한 로그인 상태를 로그인 화면·홈이 그대로 이어 봐야 한다.
 *
 * Android는 [initAuthPlatform]이 불린 뒤에 써야 한다(Context/Activity/Kakao SDK 초기화).
 */
object AuthProvider {
    val repository: AuthRepository by lazy {
        AuthRepositoryImpl(
            // TODO(서버 인증) 서버 구현(Ktor 기반 `AuthRemoteDataSource`)이 생기면 이 한 줄만 바꾼다.
            //  위쪽(AuthRepositoryImpl·화면)은 손대지 않아도 된다.
            remote = LocalAuthRemoteDataSource(),
            kakao = KakaoAuthService(),
            google = GoogleAuthService(),
        )
    }

    /** 앱 전역 로그인 상태. 라우팅은 각 플랫폼이 알아서 하고 여기서는 상태만 들고 있는다. */
    val session: AuthViewModel by lazy { AuthViewModel(repository) }
}
