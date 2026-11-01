package com.swyp.runveal.auth.storage

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Security.kSecAttrAccessibleAfterFirstUnlock

/**
 * Apple 로그인은 iOS 전용이라 expect/actual이 아니라 평범한 Kotlin/Native 클래스다
 * (Android엔 대응 클래스가 없다).
 *
 * 앱을 지웠다 다시 설치해도 유지돼야 하므로(docs/kmp_auth_native_design.md 5장)
 * `kSecAttrAccessibleAfterFirstUnlock`을 쓴다 — 기본 SecureTokenStore보다 더 넓은 접근성.
 *
 * 현재 AppleAuthService.ios.kt는 이름을 요청하지 않아(요청 scope에서 뺐음) 아무도 호출하지
 * 않는다 — 이름을 다시 요청하게 되면 여기 saveName/readName을 이어서 쓴다.
 */
@OptIn(ExperimentalForeignApi::class)
class AppleCredentialBackupStore {
    private val keychain = Keychain(
        service = "com.swyp.runveal.appleCredentialBackup",
        accessible = kSecAttrAccessibleAfterFirstUnlock,
    )

    suspend fun saveName(userId: String, name: String?) {
        if (name.isNullOrBlank()) return
        keychain.set(StorageKey.appleUserName(userId), name)
    }

    suspend fun readName(userId: String): String? = keychain.get(StorageKey.appleUserName(userId))
}
