package com.swyp.runveal.auth.service

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.swyp.runveal.auth.AndroidActivityHolder
import com.swyp.runveal.auth.AndroidAppContextHolder
import com.swyp.runveal.auth.AuthConfig
import com.swyp.runveal.auth.model.SocialAuthResult

actual class GoogleAuthService actual constructor() : SocialAuthService {
    private val credentialManager by lazy { CredentialManager.create(AndroidAppContextHolder.context) }

    actual override suspend fun signIn(): SocialAuthResult = getGoogleIdToken(filterByAuthorizedAccounts = false)

    /** 이전에 로그인했던 계정만 대상으로 조회 — UI 없이(필요하면 계정 선택만) 자동 로그인. */
    actual override suspend fun silentToken(): String? =
        (getGoogleIdToken(filterByAuthorizedAccounts = true) as? SocialAuthResult.Success)?.token

    actual override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun getGoogleIdToken(filterByAuthorizedAccounts: Boolean): SocialAuthResult {
        val activity = AndroidActivityHolder.current
            ?: return SocialAuthResult.Failure("foreground Activity 없음 — AndroidActivityHolder 미등록")
        if (AuthConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return SocialAuthResult.Failure("GOOGLE_WEB_CLIENT_ID 설정 안 됨 (local.properties)")
        }

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(AuthConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                SocialAuthResult.Success(idToken)
            } else {
                SocialAuthResult.Failure("알 수 없는 Credential 타입: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            SocialAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            if (filterByAuthorizedAccounts) SocialAuthResult.Cancelled // silentToken 쪾에선 "계정 없음"도 실패로만 처리
            else SocialAuthResult.Failure("등록된 구글 계정 없음")
        } catch (e: GetCredentialException) {
            SocialAuthResult.Failure(e.message ?: "구글 로그인 실패")
        }
    }
}
