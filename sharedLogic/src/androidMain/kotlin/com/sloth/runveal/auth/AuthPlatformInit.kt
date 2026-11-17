package com.sloth.runveal.auth

import android.app.Activity
import com.kakao.sdk.common.KakaoSdk

/**
 * androidApp의 MainActivity.onCreate에서 가장 먼저 호출해야 한다. Kakao SDK 타입을 androidApp에
 * 직접 노출하지 않으려고(의존성은 sharedLogic 안에만 두려고) 여기서 한 번에 묶는다 —
 * androidApp은 이 함수 하나만 호출하면 된다.
 */
fun initAuthPlatform(activity: Activity) {
    AndroidAppContextHolder.init(activity)
    AndroidActivityHolder.attach(activity)
    if (AuthConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
        KakaoSdk.init(activity, AuthConfig.KAKAO_NATIVE_APP_KEY)
    }
}

/** MainActivity.onDestroy에서 호출. */
fun detachAuthPlatform(activity: Activity) {
    AndroidActivityHolder.detach(activity)
}
