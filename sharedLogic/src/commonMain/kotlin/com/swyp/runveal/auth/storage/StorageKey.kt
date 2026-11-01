package com.swyp.runveal.auth.storage

/** SecureTokenStore가 다루는 키. docs/kmp_auth_native_design.md 5장 "토큰 저장소 매핑" 기준. */
object StorageKey {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val SOCIAL_LOGIN_TYPE = "social_login_type"

    /** 애플 이름 1회 제공 백업. [AppleCredentialBackupStore]가 별도로 다루며, userId별로 키가 갈린다. */
    fun appleUserName(userId: String) = "appleUserName_$userId"
    fun appleUserEmail(userId: String) = "appleUserEmail_$userId"
}
