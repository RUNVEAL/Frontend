package com.sloth.runveal.analytics

/**
 * Analytics 이벤트·파라미터 이름을 양 플랫폼이 같이 쓰는 자리.
 *
 * 이름이 한 글자라도 다르면 GA4에서 **서로 다른 이벤트로 쌓여** Android와 iOS를 비교할 수 없게
 * 된다(지표를 합치려면 그때부터 BigQuery에서 손으로 맞춰야 한다). 그래서 문자열을 각 플랫폼
 * 코드에 적지 않고 여기서만 정한다 — `MapConfig`가 지도 상수를 한곳에 두는 것과 같은 이유다.
 *
 * 수집 자체는 릴리즈 빌드에서만 켜진다(Android는 매니페스트, iOS는 Info.plist + `#if DEBUG`).
 *
 * 이름 규칙: GA4 권장대로 snake_case를 쓰고, 예약 접두사(`firebase_`/`google_`/`ga_`)는 피한다.
 */
object RunvealAnalytics {

    /**
     * 러닝 기록 화면에 들어왔다 — "달리기 시작 버튼을 눌렀다"가 아니라 **실제로 기록 화면이 열렸다**.
     * 위치 권한 시트에서 멈추면 화면이 안 열리므로 이 이벤트도 가지 않는다.
     */
    const val EVENT_RUN_START: String = "run_start"

    /** 어디서 들어온 러닝인지([SOURCE_FREE]/[SOURCE_COURSE]). */
    const val PARAM_SOURCE: String = "source"

    /** 안내 코스 없이 바로 시작한 자유 러닝(홈 줌인 화면의 러닝 시작). */
    const val SOURCE_FREE: String = "free"

    /**
     * 코스를 안내선으로 깔고 시작한 러닝 — 추천 코스 상세와 내 코스(저장한 코스) 둘 다 이 값이다.
     *
     * 둘을 갈라 보려면 `SelectedCourseHolder`가 코스 출처까지 들고 와야 한다. 지금은 경로와
     * 순환 여부만 넘기므로 기록 화면에서는 구분할 방법이 없다.
     */
    const val SOURCE_COURSE: String = "course"
}
