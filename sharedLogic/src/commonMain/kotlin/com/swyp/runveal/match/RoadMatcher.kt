package com.swyp.runveal.match

import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.LatLon

/** GPS 점 목록을 받아 도로에 맞춘 결과를 돌려주는 매처 — 구현체는 `core:tracking`의
 * `ServerRoadMatcher`(실서버 `/api/v1/match`). */
interface RoadMatcher {
    suspend fun match(points: List<LatLon>): MatchResult
}

data class MatchResult(
    val path: List<LatLon>,
    val edges: List<MatchedEdge>,
    val unmatchedPoints: List<LatLon>,
    val confidenceScore: Double,
) {
    val unmatchedPointCount: Int get() = unmatchedPoints.size
}
