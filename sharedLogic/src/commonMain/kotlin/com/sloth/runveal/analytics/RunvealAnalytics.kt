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

    /**
     * 러닝을 끝내고 **기록이 저장됐다**. 저장 기준(100m·1분, `RecordingSavePolicy`)에 못 미쳐 버려진
     * 러닝은 오지 않는다 — [EVENT_RUN_START] 대비로 완주율을 보려면 "기록이 남은 러닝"이 분모여야
     * 한다. Health Connect로 들여온 기록도 오지 않는다(앱에서 달린 게 아니다).
     */
    const val EVENT_RUN_COMPLETE: String = "run_complete"

    /** 달린 거리(m, 정수). 킬로미터 대신 미터로 보낸다 — GA4에서 합·평균을 낼 때 단위가 섞이지 않는다. */
    const val PARAM_DISTANCE_M: String = "distance_m"

    /** 걸린 시간(초, 정수). */
    const val PARAM_DURATION_SEC: String = "duration_sec"

    /** 이번에 처음 간 길의 길이(m, 정수) — 결과 화면의 "새로 탐험한 길"과 같은 값이다. */
    const val PARAM_NEW_ROAD_M: String = "new_road_m"

    /** 경로 추천을 받았다(설정 화면의 "경로 추천 받기", 결과 화면의 조건 바꿔 다시 받기 둘 다). */
    const val EVENT_COURSE_RECOMMEND: String = "course_recommend"

    /** 받아 온 코스 수. 0이면 조건에 맞는 코스가 없었다는 뜻이다([PARAM_STATUS]와 같이 봐야 한다). */
    const val PARAM_RESULT_COUNT: String = "result_count"

    /**
     * 추천 요청이 됐는지([STATUS_OK]) 실패했는지([STATUS_ERROR]).
     *
     * [PARAM_RESULT_COUNT]만으로는 "서버가 죽어서 0"과 "조건이 너무 좁아서 0"이 같아 보인다 —
     * 추천 성공률을 보려면 둘을 갈라야 한다(고쳐야 할 곳이 서버인지 추천 조건인지가 다르다).
     */
    const val PARAM_STATUS: String = "status"
    const val STATUS_OK: String = "ok"
    const val STATUS_ERROR: String = "error"

    /**
     * "이 코스로 러닝 시작"을 눌렀다 — [EVENT_RUN_START]`(source=course)`와 **다른 지점**이다.
     * 이건 버튼을 누른 순간이고, 그쪽은 기록 화면이 실제로 열린 순간이다. 둘의 차이가 위치 권한
     * 시트에서 되돌아간 사람 수다.
     */
    const val EVENT_COURSE_START: String = "course_start"

    /** 추천받은 코스에서 시작([PARAM_SOURCE]) — 추천 실사용률의 분자다. */
    const val SOURCE_RECOMMEND: String = "recommend"

    /** 저장해 둔 "내 코스"에서 시작([PARAM_SOURCE]). */
    const val SOURCE_SAVED: String = "saved"

    /** 추천 코스를 저장했다. 이미 저장한 코스를 다시 눌러도(중복 탭) 한 번만 온다. */
    const val EVENT_COURSE_SAVE: String = "course_save"

    /**
     * 위치 권한 **시스템 다이얼로그에 답했다**([PARAM_RESULT]). 설정 앱에서 바꾸고 돌아온 경우는
     * 오지 않는다 — 첫 실행 이탈 지점을 보려는 이벤트라 "물었을 때 뭐라 답했는지"만 센다.
     */
    const val EVENT_LOCATION_PERMISSION: String = "location_permission"

    /** 권한 응답 — [PERMISSION_GRANTED]/[PERMISSION_DENIED]. */
    const val PARAM_RESULT: String = "result"
    const val PERMISSION_GRANTED: String = "granted"
    const val PERMISSION_DENIED: String = "denied"
}
