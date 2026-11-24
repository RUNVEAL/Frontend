import FirebaseAnalytics
import SharedLogic

/// Analytics 이벤트를 보내는 유일한 창구 — Android `RunvealAnalyticsLogger`에 해당한다.
/// 화면/뷰모델은 Firebase를 직접 모르고 이것만 쓴다.
///
/// 이벤트 이름은 `SharedLogic`의 `RunvealAnalytics`에서 가져온다. 양 플랫폼이 같은 문자열을 써야
/// GA4에서 한 이벤트로 합쳐지기 때문에, 여기에 문자열을 다시 적지 않는다.
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
}
