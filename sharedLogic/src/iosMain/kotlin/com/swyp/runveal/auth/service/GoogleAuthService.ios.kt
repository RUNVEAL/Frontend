package com.swyp.runveal.auth.service

import com.swyp.runveal.auth.model.SocialAuthResult

/**
 * GoogleSignIn iOS SDK(GoogleSignIn.framework)는 서드파티 프레임워크라 CocoaPods/SPM으로
 * Xcode 프로젝트에 추가해야 한다. 이 레포는 macOS/Xcode가 없는 환경에서 작업돼서 그 연동을
 * 끝까지 검증할 수 없다 — 컴파일되는 틀만 두고, 실제 SDK 연동은 macOS에서 후속 작업으로
 * 진행한다. (docs/kmp_auth_native_design.md 4장 참고)
 */
actual class GoogleAuthService actual constructor() : SocialAuthService {
    actual override suspend fun signIn(): SocialAuthResult =
        SocialAuthResult.Failure("iOS GoogleSignIn SDK 연동 필요 — Xcode/CocoaPods에서 작업 필요")

    actual override suspend fun silentToken(): String? = null

    actual override suspend fun signOut() = Unit
}
