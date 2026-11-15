package com.swyp.runveal.core.map

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.dong.SegmentCoverageMatcher
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon

/** `/meta`의 manifest `matchDistanceM` 기본값(`docs/api/API.md` §3.1.2) — 매 분류 호출마다 manifest를
 * 다시 받지 않고 이 값을 쓴다. */
private const val DEFAULT_MATCH_DISTANCE_M = 15.0

/** 위도 1도 ≈ 111,320m — 바운딩 박스 사전 거르기용 대략치(경도는 위도에 따라 더 좁아지지만, 후보를
 * 넉넉하게 거르는 용도라 보수적으로 같은 비율을 쓴다). */
private const val DEGREES_PER_METER = 1.0 / 111_320.0

/**
 * 러닝 중 "오늘 달린 길"을 이미 가본 구간(흰색)/처음 가보는 구간(금색)으로 나눈다 — 서버 호출 없이
 * 전부 로컬 판정이다(`docs/api/API.md` §3.5). [road_segment] 인덱스가 비어 있으면(아직
 * [RoadSegmentIndexImporter]를 안 돌렸으면) 아무 점도 "가본 구간"으로 못 찾아서 전부 새 구간으로
 * 본다 — 크래시 대신 "전부 새 길"로 보이는 쪽을 선택한다.
 */
class LiveRoadClassifier(private val database: RunvealDatabase) {
    private val exploredSegmentIds: Set<Long> by lazy { database.exploredSegmentQueries.selectAllSegmentIds().executeAsList().toSet() }

    data class Result(val oldRuns: List<List<LatLon>>, val newRuns: List<List<LatLon>>) {
        /** 처음 가보는 구간들의 길이 합(m) — 기록 화면의 "새로 탐험한 길" 배지가 쓴다
         * ([com.swyp.runveal.result.LiveNewRoadHolder]). 지도에 금색으로 칠하는 선과 같은 근거다. */
        val newRoadMeters: Double get() = newRuns.sumOf { GeoMath.totalDistanceMeters(it) }
    }

    /** [points]를 순서대로 훑어 "이미 가본 구간 근처"인지가 바뀌는 지점마다 쪼갠 연속 구간 목록. */
    fun classify(points: List<LatLon>, matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M): Result {
        if (points.size < 2) return Result(emptyList(), emptyList())
        val oldRuns = mutableListOf<List<LatLon>>()
        val newRuns = mutableListOf<List<LatLon>>()
        var currentRun = mutableListOf(points.first())
        var currentIsOld = isOnExploredSegment(points.first(), matchDistanceM)
        for (i in 1 until points.size) {
            val point = points[i]
            val isOld = isOnExploredSegment(point, matchDistanceM)
            if (isOld != currentIsOld) {
                currentRun.add(point) // 색이 바뀌는 지점을 양쪽 구간이 공유해야 선이 안 끊겨 보인다.
                (if (currentIsOld) oldRuns else newRuns).add(currentRun)
                currentRun = mutableListOf(point)
                currentIsOld = isOld
            } else {
                currentRun.add(point)
            }
        }
        if (currentRun.size >= 2) (if (currentIsOld) oldRuns else newRuns).add(currentRun)
        return Result(oldRuns, newRuns)
    }

    /**
      * 이 점이 **지금 밟고 있는 구간**이 이미 가본 구간인지.
      *
      * 예전엔 "반경 [matchDistanceM] 안에 가본 구간이 하나라도 있으면 가본 길"로 봤는데, 그러면 왕복
      * 차로 반대편·나란한 이면도로·인도처럼 **다른 구간이 가까이 있기만 해도** 지금 처음 달리는 길이
      * 흰색으로 칠해졌다. 그래서 가본 구간만 보지 말고 **후보 전체에서 가장 가까운 구간**을 고른 뒤,
      * 그게 가본 구간일 때만 가본 길로 친다.
      */
    private fun isOnExploredSegment(point: LatLon, matchDistanceM: Double): Boolean {
        if (exploredSegmentIds.isEmpty()) return false
        val padding = matchDistanceM * DEGREES_PER_METER
        val candidates = database.roadSegmentIndexQueries.selectNear(
            maxLat = point.lat + padding,
            minLat = point.lat - padding,
            maxLon = point.lon + padding,
            minLon = point.lon - padding,
        ).executeAsList()
        if (candidates.isEmpty()) return false

        var nearestSegmentId: Long? = null
        var nearestDistance = Double.MAX_VALUE
        candidates.forEach { row ->
            val distance = SegmentCoverageMatcher.distanceToPathsMeters(
                point,
                RoadSegmentParser.decodeLines(row.coordinates),
            )
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestSegmentId = row.segment_id
            }
        }
        return nearestDistance <= matchDistanceM && nearestSegmentId in exploredSegmentIds
    }
}
