import FirebaseAnalytics
import SharedLogic

/// Analytics 이벤트를 보내는 유일한 창구 — Android `RunvealAnalyticsLogger`에 해당한다.
/// 화면/뷰모델은 Firebase를 직접 모르고 이것만 쓴다.
///
/// 이벤트 이름은 `SharedLogic`의 `RunvealAnalytics`에서 가져온다. 양 플랫폼이 같은 문자열을 써야
/// GA4에서 한 이벤트로 합쳐지기 때문에, 여기에 문자열을 다시 적지 않는다.
///
/// 링크하는 SPM product는 `FirebaseAnalytics` 하나다 — firebase-ios-sdk 13부터 이쪽이 **광고
/// 식별자(IDFA)를 쓰지 않는 기본**이고, IDFA가 필요하면 `FirebaseAnalyticsIdentitySupport`를 따로
/// 더한다(11.x의 `FirebaseAnalyticsWithoutAdIdSupport`가 기본이 된 셈이다). IDFA를 쓰면 ATT 동의
/// 흐름과 App Store 개인정보 항목이 늘어나므로 더하지 않는다.
///
/// **수집이 꺼져 있으면(디버그 빌드) SDK가 호출을 그냥 버린다** — 호출부에서 빌드 구성을 따로
/// 확인할 필요가 없다. 스위치는 Info.plist의 `FIREBASE_ANALYTICS_COLLECTION_ENABLED=false`이고,
/// 릴리즈 빌드만 `iOSApp.init`에서 다시 켠다. `GoogleService-Info.plist`가 없으면 Firebase 자체가
/// 초기화되지 않아 역시 아무 일도 일어나지 않는다.
enum RunvealAnalyticsLogger {

    /// 러닝 기록 화면 진입(`run_start`).
    ///
    /// - Parameter hasCourse: 안내 코스를 깔고 시작했는지 — 추천 코스든 저장한 코스든 true다.
    static func logRunStart(hasCourse: Bool) {
        let analytics = RunvealAnalytics.shared
        let source = hasCourse ? analytics.SOURCE_COURSE : analytics.SOURCE_FREE
        Analytics.logEvent(analytics.EVENT_RUN_START, parameters: [
            analytics.PARAM_SOURCE: source,
        ])
    }

    // MARK: - 러닝 종료·저장

    /// 러닝 종료 결과 구독 — [startWatchingRunCompletions] 참고.
    private static var runCompleteSubscription: FlowSubscription?

    /// 이미 보낸 러닝 ID. `TrackResultHolder`는 **마지막 결과를 계속 들고 있어서**, 구독을 새로
    /// 걸거나 같은 값이 다시 흐르면 한 러닝이 여러 번 셰어질 수 있다. 여기서 한 번만 통과시킨다.
    /// 앱을 끄면 홀더도 같이 비니 실행 사이에 중복될 일은 없다.
    private static var loggedRunIds: Set<String> = []

    /**
     러닝이 끝나 기록이 저장되면 `run_complete`를 보내도록 **앱 시작 때 한 번** 걸어 둔다.

     Android는 이 이벤트를 Foreground Service(`TrackingService.publishResult`)에서 쏘지만, iOS는
     그 자리가 Kotlin(`IosRunSession`)이라 Firebase를 부를 수 없다. 그래서 결과가 올라오는 홀더를
     앱 수명 내내 구독한다 — 결과 화면에 심으면 그 화면에 다시 들어올 때 또 셰어지고, 결과 화면을
     보기 전에 앱을 닫으면 아예 빠진다.

     저장 기준(100m·1분)에 못 미친 러닝은 홀더에 publish되지 않으므로 여기까지 오지 않는다.
     */
    static func startWatchingRunCompletions() {
        guard runCompleteSubscription == nil else { return }
        runCompleteSubscription = RunvealFlowWatchers.shared.watchTrackResult { result in
            guard let result else { return }
            logRunComplete(
                trackId: result.trackId,
                distanceMeters: result.distanceMeters,
                elapsedMs: result.elapsedMs,
                newRoadMeters: result.newRoadMeters
            )
        }
    }

    /// 거리·새 길은 미터 정수로, 시간은 초로 보낸다 — 소수점을 그대로 보내면 리포트에서
    /// "3.4821km"처럼 쌓여 합·평균을 보기 어렵다(Android와 같은 단위).
    private static func logRunComplete(
        trackId: String,
        distanceMeters: Double,
        elapsedMs: Int64,
        newRoadMeters: Double
    ) {
        guard loggedRunIds.insert(trackId).inserted else { return }
        let analytics = RunvealAnalytics.shared
        Analytics.logEvent(analytics.EVENT_RUN_COMPLETE, parameters: [
            analytics.PARAM_DISTANCE_M: Int(distanceMeters.rounded()),
            analytics.PARAM_DURATION_SEC: Int(elapsedMs / 1000),
            analytics.PARAM_NEW_ROAD_M: Int(newRoadMeters.rounded()),
        ])
    }

    // MARK: - 코스

    /**
     경로 추천 결과(`course_recommend`).

     - Parameter resultCount: 받아 온 코스 수. 0이면 "조건에 맞는 코스 없음"이고, **nil이면 요청
       자체가 실패**(서버·네트워크)다 — 둘을 같은 0으로 묶으면 서버 장애와 과한 추천 조건을
       구분할 수 없다.
     */
    static func logCourseRecommend(resultCount: Int?) {
        let analytics = RunvealAnalytics.shared
        Analytics.logEvent(analytics.EVENT_COURSE_RECOMMEND, parameters: [
            analytics.PARAM_RESULT_COUNT: resultCount ?? 0,
            analytics.PARAM_STATUS: resultCount == nil ? analytics.STATUS_ERROR : analytics.STATUS_OK,
        ])
    }

    /**
     "이 코스로 러닝 시작"(`course_start`).

     - Parameter fromRecommend: 추천 결과에서 바로 시작했는지 — 저장해 둔 "내 코스"에서 시작한
       것과 섞이면 추천 실사용률을 볼 수 없어서 갈라 보낸다.
     */
    static func logCourseStart(fromRecommend: Bool) {
        let analytics = RunvealAnalytics.shared
        let source = fromRecommend ? analytics.SOURCE_RECOMMEND : analytics.SOURCE_SAVED
        Analytics.logEvent(analytics.EVENT_COURSE_START, parameters: [
            analytics.PARAM_SOURCE: source,
        ])
    }

    /// 추천 코스 저장(`course_save`).
    static func logCourseSave() {
        Analytics.logEvent(RunvealAnalytics.shared.EVENT_COURSE_SAVE, parameters: nil)
    }

    // MARK: - 권한

    /// 위치 권한 다이얼로그 응답(`location_permission`).
    static func logLocationPermission(granted: Bool) {
        let analytics = RunvealAnalytics.shared
        let result = granted ? analytics.PERMISSION_GRANTED : analytics.PERMISSION_DENIED
        Analytics.logEvent(analytics.EVENT_LOCATION_PERMISSION, parameters: [
            analytics.PARAM_RESULT: result,
        ])
    }
}
