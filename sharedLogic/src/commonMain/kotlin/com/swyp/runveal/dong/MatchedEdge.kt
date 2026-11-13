package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon

/** 지도에 그릴 용도의 경로 조각 하나(좌표 목록) — 번들 샘플 트랙([MatchedEdgeParser]), 서버 매칭 결과
 * 경로, 저장된 트랙 렌더링(`TrackLocalStore.visitedRoadEdges`)이 전부 이 최소 모양을 공유한다.
 * "가본 길"로 인정됐는지·어느 구간인지는 이제 서버 `segmentId`([com.swyp.runveal.dong.RoadSegment])
 * 기준으로 따로 관리한다 — 이 타입 자체엔 식별자가 없다. */
data class MatchedEdge(
    val coordinates: List<LatLon>,
)
