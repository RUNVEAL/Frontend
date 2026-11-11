package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon

/**
 * 도로매칭 결과로 얻는 edge(도로 구간) 하나 — [DongCoverageCalculator]가 요구하는 최소 모양만 담는다.
 * 실서버 `/api/v1/match`(`com.swyp.runveal.match.RoadMatcher`)는 이어붙은 경로 하나만 주므로
 * `wayId = null`인 edge 하나로 받는다 — 그 외엔 번들 샘플 트랙(`<trackId>.matched.geojson`)을
 * [MatchedEdgeParser]로 읽어 만든 edge도 같은 모양으로 쓸 수 있다.
 *
 * @property wayId OSM way id. 같은 길을 여러 트랙에서 반복 매칭했을 때 중복 집계를 막는 키로 쓴다.
 */
data class MatchedEdge(
    val wayId: Long?,
    val coordinates: List<LatLon>,
)
