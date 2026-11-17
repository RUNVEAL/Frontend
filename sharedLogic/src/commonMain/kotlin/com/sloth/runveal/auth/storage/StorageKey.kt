package com.sloth.runveal.auth.storage

/** SecureTokenStore가 다루는 키. docs/kmp_auth_native_design.md 5장 "토큰 저장소 매핑" 기준. */
object StorageKey {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val SOCIAL_LOGIN_TYPE = "social_login_type"

    /**
     * TODO(서버 인증) 서버가 신규 여부를 내려주면 이 키와 [NICKNAME]은 지운다 — 기기별이라 계정이 바뀌어도
     * 그대로 남는 한계가 있다.
     *
     * 약관 동의·닉네임까지 끝낸 기기인지. 서버가 신규 여부([com.sloth.runveal.auth.model.Login.isNewUser])를
     * 내려줄 수 있을 때까지 그 자리를 메운다 — 가입 도중 앱이 끊겨도 다시 가입 화면으로 이어 주고,
     * 재로그인 때 약관을 또 받지 않게 한다. 계정별이 아니라 **기기별**이라는 한계가 있다.
     */
    const val SIGN_UP_COMPLETED = "sign_up_completed"

    /** 가입 때 입력한 닉네임. 서버 회원 API가 붙으면 그쪽이 원본이 된다. */
    const val NICKNAME = "nickname"

    /** 애플 이름 1회 제공 백업. [AppleCredentialBackupStore]가 별도로 다루며, userId별로 키가 갈린다. */
    fun appleUserName(userId: String) = "appleUserName_$userId"
    fun appleUserEmail(userId: String) = "appleUserEmail_$userId"
}
