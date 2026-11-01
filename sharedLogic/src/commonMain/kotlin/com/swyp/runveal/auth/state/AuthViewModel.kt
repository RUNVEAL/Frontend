package com.swyp.runveal.auth.state

import com.swyp.runveal.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * docs/kmp_auth_native_design.md 6장 "자동 로그인 · 상태 노출" 그대로 포팅.
 * 라우팅은 공유하지 않는다 — Android는 Navigation Compose, iOS는 SwiftUI 내비게이션을 각자
 * 쓰고, 여기서는 상태만 내려준다.
 */
class AuthViewModel(private val repository: AuthRepository) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Unknown)
    val state: StateFlow<AuthState> = _state

    /** 낙관적 로컬 토큰 확인으로 첫 상태를 바로 내려주고, 이후 백그라운드에서 실제 세션을 복구한다. */
    suspend fun bootstrap() {
        val token = repository.getAccessToken()
        _state.value = if (token.isNullOrEmpty()) AuthState.LoggedOut else AuthState.LoggedIn

        val recovered = runCatching { repository.recoverSession() }.getOrNull()
        when {
            recovered != null -> _state.value = AuthState.LoggedIn
            token != null -> _state.value = AuthState.LoggedOut
            // 네트워크 등 일시 오류는 로그인 상태 유지 — 이후 401에서 처리.
        }
    }

    /** 로그인/로그아웃 직후 상태를 동기적으로 확정한다 (비동기 재계산 X) — stale 상태로 인한
     * redirect 가드 오동작을 막기 위함. */
    fun markLoggedIn() {
        _state.value = AuthState.LoggedIn
    }

    fun markLoggedOut() {
        _state.value = AuthState.LoggedOut
    }
}
