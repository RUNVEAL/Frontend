package com.swyp.runveal.data.local

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.dong.SegmentCoverageMatcher
import com.swyp.runveal.map.LatLon

/** 위도 1도 ≈ 111,320m — 바운딩 박스로 후보를 추릴 때 쓰는 대략치. */
private const val DEGREES_PER_METER = 1.0 / 111_320.0

/**
 * "이 점은 **어느 도로 구간 위에 있나**"를 로컬 `road_segment` 인덱스로 판정한다.
 *
 * 러닝 중 흰색/금색 구분([com.swyp.runveal.core.map.LiveRoadClassifier])과 러닝이 끝난 뒤의 구간
 * 적립([TrackLocalStore.segmentIdsOnPaths])이 **같은 규칙**을 쓰게 하려고 여기 한 곳에 모았다. 규칙이
 * 갈리면 "러닝 중엔 새 길(금색)이었는데 결과·탐험률엔 안 잡히는" 어긋남이 생긴다.
 *
 * 가장 가까운 구간 **하나**만 고르는 게 핵심이다. "반경 안에 구간이 있나"로 보면 왕복 차로 반대편·
 * 인도·나란한 이면도로가 같이 잡혀서, 지금 달리는 길이 아닌 구간을 밟은 것으로 친다.
 */
class RoadSegmentLocator(
    private val database: RunvealDatabase,
    private val matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M,
) {
    /** 디코딩이 비싸서(구간마다 MultiLineString JSON) 한 번 푼 건 들고 있는다. */
    private val decodedLines = mutableMapOf<Long, List<List<LatLon>>>()

    /** [point]에서 [matchDistanceM] 안에 있는 구간 중 **가장 가까운** 것의 ID. 없으면 null. */
    fun nearestSegmentId(point: LatLon): Long? {
        val padding = matchDistanceM * DEGREES_PER_METER
        val candidates = database.roadSegmentIndexQueries.selectNear(
            maxLat = point.lat + padding,
            minLat = point.lat - padding,
            maxLon = point.lon + padding,
            minLon = point.lon - padding,
        ).executeAsList()
        if (candidates.isEmpty()) return null

        var nearestId: Long? = null
        var nearestDistance = Double.MAX_VALUE
        candidates.forEach { row ->
            val lines = decodedLines.getOrPut(row.segment_id) { RoadSegmentParser.decodeLines(row.coordinates) }
            val distance = SegmentCoverageMatcher.distanceToPathsMeters(point, lines)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestId = row.segment_id
            }
        }
        return nearestId?.takeIf { nearestDistance <= matchDistanceM }
    }

    companion object {
        /** `/meta` manifest `matchDistanceM` 기본값(`docs/api/API.md` §3.1.2). */
        const val DEFAULT_MATCH_DISTANCE_M = 15.0
    }
}
