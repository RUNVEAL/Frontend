package com.sloth.runveal.auth.storage

/** 토큰·소셜 로그인 타입을 암호화 저장. Android는 EncryptedSharedPreferences, iOS는 Keychain. */
expect class SecureTokenStore() {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun delete(key: String)
}
