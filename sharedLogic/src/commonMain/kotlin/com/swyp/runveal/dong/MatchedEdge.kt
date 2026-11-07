package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon

/**
 * 도로매칭 결과로 얻는 edge(도로 구간) 하나 — [DongCoverageCalculator]가 요구하는 최소 모양만 담는다.
 * 실제 도로매칭(Valhalla, Ktor 재작성 예정)은 이번 범위에 없어서, 지금은 [com.swyp.runveal.map.SAMPLE_VISITED_ROUTE]
 * 같은 샘플 좌표로 만든 edge를 넘겨 완주율 계산을 시연한다.
 *
 * @property wayId OSM way id. 같은 길을 여러 트랙에서 반복 매칭했을 때 중복 집계를 막는 키로 쓴다.
 */
data class MatchedEdge(
    val wayId: Long?,
    val coordinates: List<LatLon>,
)
