package com.sloth.runveal.analytics

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import kotlin.math.roundToLong

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

    /**
     * 러닝 종료·저장([RunvealAnalytics.EVENT_RUN_COMPLETE]).
     *
     * 거리·새 길은 미터 정수로, 시간은 초로 보낸다 — 소수점을 그대로 보내면 리포트에서
     * "3.4821km"처럼 쌓여 합·평균을 보기 어렵다.
     */
    fun logRunComplete(distanceMeters: Double, elapsedMs: Long, newRoadMeters: Double) {
        Firebase.analytics.logEvent(RunvealAnalytics.EVENT_RUN_COMPLETE) {
            param(RunvealAnalytics.PARAM_DISTANCE_M, distanceMeters.roundToLong())
            param(RunvealAnalytics.PARAM_DURATION_SEC, elapsedMs / 1000)
            param(RunvealAnalytics.PARAM_NEW_ROAD_M, newRoadMeters.roundToLong())
        }
    }

    /**
     * 경로 추천 결과([RunvealAnalytics.EVENT_COURSE_RECOMMEND]).
     *
     * @param resultCount 받아 온 코스 수. 0이면 "조건에 맞는 코스 없음"이고, **null이면 요청 자체가
     *   실패**(서버·네트워크)다 — 둘을 같은 0으로 묶으면 서버 장애와 과한 추천 조건을 구분할 수 없다.
     */
    fun logCourseRecommend(resultCount: Int?) {
        Firebase.analytics.logEvent(RunvealAnalytics.EVENT_COURSE_RECOMMEND) {
            param(RunvealAnalytics.PARAM_RESULT_COUNT, (resultCount ?: 0).toLong())
            param(
                RunvealAnalytics.PARAM_STATUS,
                if (resultCount == null) RunvealAnalytics.STATUS_ERROR else RunvealAnalytics.STATUS_OK,
            )
        }
    }

    /**
     * "이 코스로 러닝 시작"([RunvealAnalytics.EVENT_COURSE_START]).
     *
     * @param fromRecommend 추천 결과에서 바로 시작했는지 — 저장해 둔 "내 코스"에서 시작한 것과
     *   섞이면 추천 실사용률을 볼 수 없어서 갈라 보낸다.
     */
    fun logCourseStart(fromRecommend: Boolean) {
        val source =
            if (fromRecommend) RunvealAnalytics.SOURCE_RECOMMEND else RunvealAnalytics.SOURCE_SAVED
        Firebase.analytics.logEvent(RunvealAnalytics.EVENT_COURSE_START) {
            param(RunvealAnalytics.PARAM_SOURCE, source)
        }
    }

    /** 추천 코스 저장([RunvealAnalytics.EVENT_COURSE_SAVE]). */
    fun logCourseSave() {
        Firebase.analytics.logEvent(RunvealAnalytics.EVENT_COURSE_SAVE) {}
    }

    /** 위치 권한 다이얼로그 응답([RunvealAnalytics.EVENT_LOCATION_PERMISSION]). */
    fun logLocationPermission(granted: Boolean) {
        val result =
            if (granted) RunvealAnalytics.PERMISSION_GRANTED else RunvealAnalytics.PERMISSION_DENIED
        Firebase.analytics.logEvent(RunvealAnalytics.EVENT_LOCATION_PERMISSION) {
            param(RunvealAnalytics.PARAM_RESULT, result)
        }
    }
}
