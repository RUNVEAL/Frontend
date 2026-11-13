package com.swyp.runveal.match

import com.swyp.runveal.map.LatLon

/** GPS 점 목록을 받아 도로에 맞춘 결과를 돌려주는 매처 — 구현체는 `core:tracking`의
 * `ServerRoadMatcher`(실서버 `/api/v1/match`). */
interface RoadMatcher {
    suspend fun match(points: List<LatLon>): MatchResult
}

/** [path]는 도로에 맞춘 경로선(`[lng, lat]` → [LatLon]), [segmentIds]는 그 경로가 지나는 서버
 * 구간 ID(`docs/api/API.md` §3.6.2) — "가본 길" 인정은 이 ID를 기준으로 한다
 * ([com.swyp.runveal.dong.RoadSegment]). */
data class MatchResult(
    val path: List<LatLon>,
    val segmentIds: List<Long>,
)
