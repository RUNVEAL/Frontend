package com.sloth.runveal.auth

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * docs/kmp_auth_native_design.md 3장 "유의점"이 언급한 미해결 과제(Activity 주입)에 대한
 * 구체적인 해법. CredentialManager.getCredential()과 Firebase
 * startActivityForSignInWithProvider() 모두 Activity 컨텍스트가 필요하지만, `expect class
 * KakaoAuthService()`처럼 commonMain의 expect 생성자는 인자가 없어야 하므로 Activity를
 * 생성자로 주입할 수 없다.
 *
 * 그래서 현재 foreground Activity를 들고 있는 전역 홀더를 둔다. androidApp의
 * `MainActivity.onCreate`/`onDestroy`에서 등록/해제한다. WeakReference라 Activity가
 * 내려가도(화면 꺼짐, 백그라운드) 메모리에 붙잡아두지 않는다.
 */
object AndroidActivityHolder {
    private var ref: WeakReference<Activity>? = null

    val current: Activity?
        get() = ref?.get()

    fun attach(activity: Activity) {
        ref = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }
}
