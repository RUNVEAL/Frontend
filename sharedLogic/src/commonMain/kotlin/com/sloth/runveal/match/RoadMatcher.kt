package com.sloth.runveal.match

import com.sloth.runveal.map.LatLon

/** GPS 점 목록을 받아 도로에 맞춘 결과를 돌려주는 매처 — 구현체는 `core:tracking`의
 * `ServerRoadMatcher`(실서버 `/api/v1/match`). */
interface RoadMatcher {
    suspend fun match(points: List<LatLon>): MatchResult
}

/** [path]는 도로에 맞춘 경로선(`[lng, lat]` → [LatLon]), [segmentIds]는 그 경로가 지나는 서버
 * 구간 ID(`docs/api/API.md` §3.6.2) — "가본 길" 인정은 이 ID를 기준으로 한다
 * ([com.sloth.runveal.dong.RoadSegment]). */
data class MatchResult(
    val path: List<LatLon>,
    val segmentIds: List<Long>,
    /** 이번 러닝이 지나간 시군구 코드(5자리). 전국 구간 인덱스를 통째로 만드는 대신 이 구들의
     * `segments-{구코드}.geojson`만 골라 들여오는 데 쓴다(`RoadSegmentIndexImporter`). */
    val sigunguCodes: List<String> = emptyList(),
)
