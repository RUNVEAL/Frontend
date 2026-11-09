package com.swyp.runveal.valhalla

import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.LatLon

/** `feature-valhalla-matching.md` "MockRoadMatcher" — 원안은 `assets/mock/trace_response.json`을 읽지만,
 * KMP(commonMain)에서는 플랫폼별 asset 접근이 필요해 단순화했다: 입력 좌표를 그대로 길 하나짜리
 * [MatchedEdge](`wayId = null`)로 돌려준다 — 서버 없이도 [RoadMatcher] 소비 쪽(청크 합치기, 저장)을
 * 테스트하는 용도로는 충분하다. */
class MockRoadMatcher : RoadMatcher {
    override suspend fun match(points: List<LatLon>): MatchResult = MatchResult(
        path = points,
        edges = if (points.size >= 2) listOf(MatchedEdge(wayId = null, coordinates = points)) else emptyList(),
        unmatchedPoints = emptyList(),
        confidenceScore = 1.0,
    )
}
