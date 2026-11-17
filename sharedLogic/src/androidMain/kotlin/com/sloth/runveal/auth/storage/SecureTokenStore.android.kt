package com.sloth.runveal.auth.storage

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sloth.runveal.auth.AndroidAppContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class SecureTokenStore actual constructor() {
    private val prefs by lazy { encryptedPrefs(FILE_NAME) }

    actual suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)
    }

    actual suspend fun write(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, value).apply()
    }

    actual suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val FILE_NAME = "runveal_secure_token_store"
    }
}

/** Android 쪽 두 보안 저장소(SecureTokenStore, AppleCredentialBackupStore)가 공유하는 헬퍼. */
internal fun encryptedPrefs(fileName: String): android.content.SharedPreferences {
    val context = AndroidAppContextHolder.context
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        fileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
