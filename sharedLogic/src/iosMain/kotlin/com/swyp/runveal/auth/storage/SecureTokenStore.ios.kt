package com.swyp.runveal.auth.storage

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class SecureTokenStore actual constructor() {
    private val keychain = Keychain(service = "com.swyp.runveal.secureTokenStore")

    actual suspend fun read(key: String): String? = keychain.get(key)

    actual suspend fun write(key: String, value: String) = keychain.set(key, value)

    actual suspend fun delete(key: String) = keychain.delete(key)
}
