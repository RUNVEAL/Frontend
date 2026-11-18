package com.sloth.runveal.external

import com.sloth.runveal.map.LatLon

/**
 * 외부 앱(Health Connect 등)에 기록된 운동 한 건. 가져오기 파이프라인([ExternalWorkoutImporter])이
 * 다루는 최소 정보만 담는다 — 심박·칼로리처럼 Runveal이 쓰지 않는 값은 읽지 않는다.
 *
 * [route]가 비어 있으면 도로 매칭을 할 수 없어서 가져올 수 없다. 다만 "경로가 아예 없는" 것과
 * "있는데 아직 동의를 못 받은" 것은 다르다 — 후자는 [needsRouteConsent]로 가려내 동의를 받으면
 * 읽을 수 있다.
 */
data class ExternalWorkout(
    /** 외부 앱이 부여한 원본 ID(Health Connect 세션 UID). 중복 가져오기를 막는 기준이다. */
    val externalId: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    /** 외부 앱이 기록한 거리(m). 없으면 null — 그때는 매칭된 경로 길이로 대신한다. */
    val distanceMeters: Double?,
    /** GPS 경로. 시간순으로 정렬돼 있어야 한다. 동의를 못 받았으면 비어 있다. */
    val route: List<ExternalWorkoutPoint>,
    /**
     * 경로가 **있지만 동의가 필요해** 못 읽은 상태. 다른 앱이 기록한 경로가 여기 해당한다
     * (`ExerciseRouteResult.ConsentRequired`). 세션별 동의 화면을 띄우면 읽을 수 있다.
     *
     * 애초에 경로 없이 기록된 세션(실내 러닝 등)은 false — 물어봐야 소용없다.
     */
    val needsRouteConsent: Boolean = false,
) {
    val elapsedMs: Long get() = finishedAtEpochMs - startedAtEpochMs
}

/** 경로의 한 점. 10초 간격 축약([MatchPointReducer])에 시각이 필요해서 좌표만 받지 않는다. */
data class ExternalWorkoutPoint(
    val lat: Double,
    val lon: Double,
    val timeEpochMs: Long,
) {
    fun toLatLon(): LatLon = LatLon(lat = lat, lon = lon)
}
