package com.sloth.runveal.auth.state

sealed interface AuthState {
    data object LoggedIn : AuthState
    data object LoggedOut : AuthState
    data object Unknown : AuthState
}
