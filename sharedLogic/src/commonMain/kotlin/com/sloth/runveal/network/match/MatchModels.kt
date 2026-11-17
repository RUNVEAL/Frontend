package com.sloth.runveal.network.match

import kotlinx.serialization.Serializable

/** `POST /api/v1/match` 요청 — `docs/api/API.md` §3.6.1/§3.7. `points`는 `[lng, lat]` 배열(§5.2 좌표
 * 순서 주의 — `/routes`의 `start`/`end`와 반대), 10~2,000개(`/meta`의 `limits.match`). */
@Serializable
data class MatchRequest(
    val mapVersion: String,
    val purpose: MatchPurpose,
    val points: List<List<Double>>,
)

/** `LIVE`(방금 끝난 러닝) · `IMPORT`(외부 기록 가져오기) · `REMATCH`(지도 교체로 다시 계산).
 * `IMPORT`/`REMATCH`는 서버 전체에서 동시에 1건만 처리된다(§3.7). */
@Serializable
enum class MatchPurpose {
    LIVE, IMPORT, REMATCH
}

/** `POST /api/v1/match` 응답. */
@Serializable
data class MatchResponse(
    val mapVersion: String,
    val segmentIds: List<Long>,
    /** `[lng, lat]` — GeoJSON 좌표 순서(§5.2). */
    val matchedPath: List<List<Double>>,
    val matchedDistanceM: Int,
    val sigunguCodes: List<String>,
    /** 구간 ID(문자열 키) → 행정동 코드. 동을 못 찾은 구간은 빠질 수 있다. */
    val dongBySegment: Map<String, String>,
)
