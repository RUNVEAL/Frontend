package com.sloth.runveal.auth

import android.content.Context

/**
 * [SecureTokenStore]/[AppleCredentialBackupStore]의 `expect class` 생성자가 인자를 받을 수
 * 없어서(commonMain과 시그니처를 맞춰야 함) ApplicationContext를 여기 전역으로 들고 있는다.
 * androidApp의 MainActivity.onCreate에서 가장 먼저 [init]을 호출해야 한다.
 */
object AndroidAppContextHolder {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val context: Context
        get() = appContext
            ?: error("AndroidAppContextHolder.init(context)를 MainActivity.onCreate에서 먼저 호출해야 합니다.")
}
