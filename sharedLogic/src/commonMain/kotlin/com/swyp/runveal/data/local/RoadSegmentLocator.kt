package com.swyp.runveal.data.local

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.dong.SegmentCoverageMatcher
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon

/** 위도 1도 ≈ 111,320m — 바운딩 박스로 후보를 추릴 때 쓰는 대략치. */
private const val DEGREES_PER_METER = 1.0 / 111_320.0

/**
 * "이 점은 **어느 도로 구간의 어디쯤** 위에 있나"를 로컬 `road_segment` 인덱스로 판정한다.
 *
 * 러닝 중 흰색/금색 구분([com.swyp.runveal.core.map.LiveRoadClassifier])과 러닝이 끝난 뒤의 구간
 * 적립([TrackLocalStore.coverageOnPaths])이 **같은 규칙**을 쓰게 하려고 여기 한 곳에 모았다. 규칙이
 * 갈리면 "러닝 중엔 새 길(금색)이었는데 결과·탐험률엔 안 잡히는" 어긋남이 생긴다.
 *
 * 가장 가까운 구간 **하나**만 고르는 게 핵심이다. "반경 안에 구간이 있나"로 보면 왕복 차로 반대편·
 * 인도·나란한 이면도로가 같이 잡혀서, 지금 달리는 길이 아닌 구간을 밟은 것으로 친다.
 *
 * 구간 안의 **위치**([SegmentHit.sampleIndex])까지 돌려주는 건 적립을 구간 전체가 아니라 샘플 단위로
 * 쪼개기 위해서다([com.swyp.runveal.dong.CoverageMask]).
 */
class RoadSegmentLocator(
    private val database: RunvealDatabase,
    private val matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M,
) {
    /** 디코딩이 비싸서(구간마다 MultiLineString JSON) 한 번 푼 건 들고 있는다. */
    private val decodedLines = mutableMapOf<Long, List<List<LatLon>>>()
    private val sampledLines = mutableMapOf<Long, List<List<LatLon>>>()

    /** 점이 올라탄 구간과 그 구간 안에서 가장 가까운 샘플 번호. */
    data class SegmentHit(val segmentId: Long, val sampleIndex: Int, val sampleCount: Int)

    /** [point]에서 [matchDistanceM] 안에 있는 구간 중 **가장 가까운** 것의 ID. 없으면 null. */
    fun nearestSegmentId(point: LatLon): Long? = locate(point)?.segmentId

    /** [nearestSegmentId] + 그 구간 안에서 점에 가장 가까운 샘플 위치. */
    fun locate(point: LatLon): SegmentHit? {
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
        val segmentId = nearestId?.takeIf { nearestDistance <= matchDistanceM } ?: return null

        val samples = samplePoints(segmentId)
        if (samples.isEmpty()) return null
        var sampleIndex = 0
        var sampleDistance = Double.MAX_VALUE
        samples.forEachIndexed { index, sample ->
            val distance = GeoMath.distanceMeters(point, sample)
            if (distance < sampleDistance) {
                sampleDistance = distance
                sampleIndex = index
            }
        }
        return SegmentHit(segmentId, sampleIndex, samples.size)
    }

    /** [segmentId]의 샘플 점 — 조각(MultiLineString)별로 나눈 그대로. 비트 순서는 이어 붙인 순서다. */
    fun samplePointsByLine(segmentId: Long): List<List<LatLon>> = sampledLines.getOrPut(segmentId) {
        SegmentCoverageMatcher.samplePointsByLine(linesFor(segmentId))
    }

    fun samplePoints(segmentId: Long): List<LatLon> = samplePointsByLine(segmentId).flatten()

    private fun linesFor(segmentId: Long): List<List<LatLon>> = decodedLines.getOrPut(segmentId) {
        val row = database.roadSegmentIndexQueries.selectById(segmentId).executeAsOneOrNull()
        if (row == null) emptyList() else RoadSegmentParser.decodeLines(row.coordinates)
    }

    companion object {
        /** `/meta` manifest `matchDistanceM` 기본값(`docs/api/API.md` §3.1.2). */
        const val DEFAULT_MATCH_DISTANCE_M = 15.0
    }
}
