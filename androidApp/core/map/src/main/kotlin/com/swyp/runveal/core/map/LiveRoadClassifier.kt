package com.swyp.runveal.core.map

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.dong.SegmentCoverageMatcher
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

    data class Result(val oldRuns: List<List<LatLon>>, val newRuns: List<List<LatLon>>)

    /** [points]를 순서대로 훑어 "이미 가본 구간 근처"인지가 바뀌는 지점마다 쪼갠 연속 구간 목록. */
    fun classify(points: List<LatLon>, matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M): Result {
        if (points.size < 2) return Result(emptyList(), emptyList())
        val oldRuns = mutableListOf<List<LatLon>>()
        val newRuns = mutableListOf<List<LatLon>>()
        var currentRun = mutableListOf(points.first())
        var currentIsOld = isNearExploredSegment(points.first(), matchDistanceM)
        for (i in 1 until points.size) {
            val point = points[i]
            val isOld = isNearExploredSegment(point, matchDistanceM)
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

    private fun isNearExploredSegment(point: LatLon, matchDistanceM: Double): Boolean {
        if (exploredSegmentIds.isEmpty()) return false
        val padding = matchDistanceM * DEGREES_PER_METER
        val candidates = database.roadSegmentIndexQueries.selectNear(
            maxLat = point.lat + padding,
            minLat = point.lat - padding,
            maxLon = point.lon + padding,
            minLon = point.lon - padding,
        ).executeAsList().filter { it.segment_id in exploredSegmentIds }
        if (candidates.isEmpty()) return false
        return candidates.any { row ->
            SegmentCoverageMatcher.isNearAnyPath(point, RoadSegmentParser.decodeLines(row.coordinates), matchDistanceM)
        }
    }
}
