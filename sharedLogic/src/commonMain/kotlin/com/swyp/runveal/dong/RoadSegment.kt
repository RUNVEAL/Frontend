package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon

/** 서버 지도 파일 `segments-{구코드}.geojson`(`docs/api/API.md` §3.1.2) 구간 하나. `geometry.type`이
 * `MultiLineString`이라 [lines]가 여러 조각으로 올 수 있다. */
data class RoadSegment(
    val segmentId: Long,
    val lengthM: Int,
    val lines: List<List<LatLon>>,
)
