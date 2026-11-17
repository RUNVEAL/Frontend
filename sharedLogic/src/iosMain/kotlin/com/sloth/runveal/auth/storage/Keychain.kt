package com.sloth.runveal.auth.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS Keychain 위에 올린 간단한 String key-value 저장소. `SecureTokenStore`/
 * `AppleCredentialBackupStore`가 공유한다. docs/kmp_auth_native_design.md 5장이 요구하는
 * "앱 삭제 후에도 유지"는 [accessible]을 `kSecAttrAccessibleAfterFirstUnlock`으로 바꾸면 된다
 * (기본은 더 보수적인 `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`).
 *
 * macOS/Xcode가 없는 환경에서 작성해 `compileKotlinIosArm64`로 타입 체크만 확인했다 — 실제
 * 기기/시뮬레이터에서의 동작 검증은 아직 못 했다.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class Keychain(
    private val service: String,
    private val accessible: COpaquePointer? = kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
) {
    fun set(key: String, value: String) {
        val data = value.toNSData()
        val updateDict = newDict(1).apply {
            addValue(kSecValueData, data.toCFRetained())
        }
        val updateStatus = SecItemUpdate(queryDict(key), updateDict)
        if (updateStatus == errSecItemNotFound) {
            val addDict = queryDict(key).apply {
                addValue(kSecValueData, data.toCFRetained())
                addValue(kSecAttrAccessible, accessible)
            }
            SecItemAdd(addDict, null)
        }
    }

    fun get(key: String): String? = memScoped {
        val dict = queryDict(key).apply {
            addValue(kSecReturnData, true.toCFBoolean())
            addValue(kSecMatchLimit, kSecMatchLimitOne)
        }
        val result = alloc<COpaquePointerVar>()
        val status = SecItemCopyMatching(dict, result.ptr)
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        data.toKString()
    }

    fun delete(key: String) {
        SecItemDelete(queryDict(key))
    }

    private fun queryDict(key: String): CFMutableDictionaryRef? = newDict(3).apply {
        addValue(kSecClass, kSecClassGenericPassword)
        addValue(kSecAttrService, service.toCFRetained())
        addValue(kSecAttrAccount, key.toCFRetained())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun newDict(capacity: Long): CFMutableDictionaryRef? =
    CFDictionaryCreateMutable(null, capacity.convert(), null, null)

@OptIn(ExperimentalForeignApi::class)
private fun CFMutableDictionaryRef?.addValue(key: CValuesRef<*>?, value: CValuesRef<*>?) {
    CFDictionaryAddValue(this, key, value)
}

/** Kotlin String/Boolean 등은 CFTypeRef가 아니라 CFBridgingRetain으로 한 번 감싸야 한다. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun Any.toCFRetained(): COpaquePointer? = CFBridgingRetain(this)

@OptIn(ExperimentalForeignApi::class)
private fun Boolean.toCFBoolean(): COpaquePointer? =
    if (this) platform.CoreFoundation.kCFBooleanTrue else platform.CoreFoundation.kCFBooleanFalse

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun String.toNSData(): NSData {
    val bytes = encodeToByteArray()
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.convert())
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toKString(): String? =
    NSString.create(data = this, encoding = NSUTF8StringEncoding) as String?
