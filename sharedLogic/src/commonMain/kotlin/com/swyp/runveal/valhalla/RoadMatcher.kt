package com.swyp.runveal.valhalla

import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.LatLon

/** `feature-valhalla-matching.md` "인터페이스: RoadMatcher". */
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

/** `ValhallaConfig.USE_MOCK`에 따라 구현체를 고른다(§설정 "MapViewModel 생성자에서 분기"). */
fun defaultRoadMatcher(): RoadMatcher = if (ValhallaConfig.USE_MOCK) MockRoadMatcher() else RemoteRoadMatcher()
