package com.sloth.runveal.auth.service

import com.sloth.runveal.auth.model.SocialAuthResult
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.darwin.NSObject

/**
 * Apple 로그인은 iOS 전용이라 expect/actual이 아니라 평범한 Kotlin/Native 클래스다
 * (Android엔 대응 클래스가 없다 — AuthRepositoryImpl도 이 클래스를 모른다).
 *
 * `AuthenticationServices`는 Kotlin/Native가 기본 제공하는 시스템 프레임워크라 CocoaPods 없이
 * 실제 구현했다 — nonce 생성/해시, ASAuthorizationController 델리게이트 플로우까지.
 *
 * 다만 docs/kmp_auth_native_design.md가 요구하는 "Firebase 자격증명 교환"은 Firebase iOS
 * SDK(서드파티, CocoaPods/SPM 필요)가 있어야 하는데 이 환경엔 Xcode가 없어 추가할 수 없다.
 * 그래서 Apple 자격증명(identityToken)을 받는 데까지만 실제로 동작하고, 그 다음 Firebase
 * 교환 단계는 TODO로 남겨 Xcode에서 Firebase SDK를 추가한 뒤 이어서 구현해야 한다.
 */
@OptIn(ExperimentalForeignApi::class)
class AppleAuthService : SocialAuthService {
    override suspend fun signIn(): SocialAuthResult {
        val rawNonce = AppleNonce.random()
        val hashedNonce = AppleNonce.sha256Hex(rawNonce)

        val credentialResult = requestAppleIdCredential(hashedNonce)
        val credential = credentialResult.getOrElse { error ->
            return if (error is SignInCancelled) {
                SocialAuthResult.Cancelled
            } else {
                SocialAuthResult.Failure(error.message ?: "Apple 로그인 실패")
            }
        }

        val identityToken = credential.identityToken?.let { NSStringFromData(it) }
            ?: return SocialAuthResult.Failure("identityToken 없음")

        // TODO(Firebase iOS SDK 필요): OAuthProvider.credential(withProviderID: "apple.com",
        //  idToken: identityToken, rawNonce: rawNonce)로 만든 자격증명을
        //  Auth.auth().signIn(with:)에 넘겨 Firebase idToken으로 교환해야 한다.
        //  CocoaPods/SPM으로 FirebaseAuth를 추가한 뒤(이 환경엔 Xcode가 없어 못 함) 이어서 구현.
        return SocialAuthResult.Failure(
            "Apple 자격증명 획득 성공, Firebase 교환 미구현 (Firebase iOS SDK는 Xcode에서 추가 필요)",
        )
    }

    /** Apple엔 iOS AuthenticationServices 쪾의 "무중단 세션 복원" 전용 API가 따로 없다 —
     * Firebase currentUser의 idToken 재발급에 의존해야 하는데, 위와 같은 이유로 아직 미구현. */
    override suspend fun silentToken(): String? = null

    override suspend fun signOut() = Unit

    /** 회원탈퇴 재인증용 — scope 없이 재인증만 수행, 5분/1회용이므로 받는 즉시 서버로 보내야 한다. */
    suspend fun authorizationCodeForRevoke(): String? {
        val credential = requestAppleIdCredential(hashedNonce = null, forRevoke = true).getOrNull() ?: return null
        return credential.authorizationCode?.let { NSStringFromData(it) }
    }

    private suspend fun requestAppleIdCredential(
        hashedNonce: String?,
        forRevoke: Boolean = false,
    ): Result<ASAuthorizationAppleIDCredential> = suspendCancellableCoroutine { cont ->
        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest()
        if (!forRevoke) {
            request.requestedScopes = listOf(ASAuthorizationScopeEmail)
            request.nonce = hashedNonce
        }

        val delegate = AppleAuthDelegate { result -> if (!cont.isCompleted) cont.resume(result) }
        val controller = ASAuthorizationController(authorizationRequests = listOf(request))
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
    }
}

private class SignInCancelled : Exception("사용자가 Apple 로그인을 취소함")

@OptIn(ExperimentalForeignApi::class)
private class AppleAuthDelegate(
    private val onResult: (Result<ASAuthorizationAppleIDCredential>) -> Unit,
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        if (credential != null) {
            onResult(Result.success(credential))
        } else {
            onResult(Result.failure(Exception("알 수 없는 Apple 자격증명 타입")))
        }
    }

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
        val cancelled = didCompleteWithError.domain == "com.apple.AuthenticationServices.AuthorizationError" &&
            didCompleteWithError.code == 1001L // ASAuthorizationErrorCanceled
        onResult(Result.failure(if (cancelled) SignInCancelled() else Exception(didCompleteWithError.localizedDescription)))
    }

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        return UIApplication.sharedApplication.windows.firstOrNull() as? ASPresentationAnchor
            ?: error("표시할 UIWindow를 찾지 못함")
    }
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
private fun NSStringFromData(data: NSData): String? =
    NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
