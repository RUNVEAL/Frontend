package com.sloth.runveal.analytics

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * Analytics 이벤트를 보내는 유일한 창구. 화면/ViewModel은 Firebase를 직접 모르고 이것만 쓴다.
 *
 * **수집이 꺼져 있으면(디버그 빌드) SDK가 호출을 그냥 버린다** — 호출부에서 빌드 타입을 따로
 * 확인할 필요가 없다. 스위치는 AndroidManifest의 `firebase_analytics_collection_enabled`이고
 * 값은 `androidApp/app/build.gradle.kts`의 buildTypes가 넣는다(release만 true).
 *
 * `google-services.json`이 없으면 Firebase 자체가 초기화되지 않아 역시 아무 일도 일어나지 않는다.
 * 어느 경우에도 예외를 던지지 않으므로 호출부에 방어 코드를 두지 않아도 된다.
 */
object RunvealAnalyticsLogger {

    /**
     * 러닝 기록 화면 진입([RunvealAnalytics.EVENT_RUN_START]).
     *
     * @param hasCourse 안내 코스를 깔고 시작했는지 — 추천 코스든 저장한 코스든 true다.
     */
    fun logRunStart(hasCourse: Boolean) {
        val source = if (hasCourse) RunvealAnalytics.SOURCE_COURSE else RunvealAnalytics.SOURCE_FREE
        Firebase.analytics.logEvent(RunvealAnalytics.EVENT_RUN_START) {
            param(RunvealAnalytics.PARAM_SOURCE, source)
        }
    }
}
