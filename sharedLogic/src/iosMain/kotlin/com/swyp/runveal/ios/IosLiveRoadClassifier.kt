package com.swyp.runveal.ios

import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.dong.SegmentCoverageMatcher
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon

/** `/meta`의 manifest `matchDistanceM` 기본값(`docs/api/API.md` §3.1.2). */
private const val DEFAULT_MATCH_DISTANCE_M = 15.0

/** 위도 1도 ≈ 111,320m — 바운딩 박스 사전 거르기용 대략치. */
private const val DEGREES_PER_METER = 1.0 / 111_320.0

/**
 * 러닝 중 "오늘 달린 길"을 이미 가본 구간(흰색)/처음 가보는 구간(금색)으로 나눈다 — 서버 호출 없이
 * 전부 로컬 판정이다(`docs/api/API.md` §3.5). Android `core:map`의 `LiveRoadClassifier`와 같은 로직이며,
 * 입력·출력을 Swift가 바로 쓸 수 있는 GeoJSON 문자열까지 만들어 준다.
 *
 * `road_segment` 인덱스가 비어 있으면(아직 [IosRoadSegmentIndexImporter]를 안 돌렸으면) 아무 점도
 * "가본 구간"으로 못 찾아서 전부 새 구간으로 본다 — 크래시 대신 "전부 새 길"로 보이는 쪽을 택한다.
 */
class IosLiveRoadClassifier(
    private val database: RunvealDatabase = RunvealDatabaseProvider.database,
) {
    private val exploredSegmentIds: Set<Long> by lazy {
        database.exploredSegmentQueries.selectAllSegmentIds().executeAsList().toSet()
    }

    /** 지도에 바로 넣을 수 있게 GeoJSON까지 만든 분류 결과. */
    data class Result(
        /** 이미 가본 구간(흰색) 선 레이어용 GeoJSON. */
        val oldGeoJson: String,
        /** 처음 가보는 구간(금색) 선 레이어용 GeoJSON. */
        val newGeoJson: String,
        /** 처음 가보는 구간들의 길이 합(m) — 기록 화면 "새로 탐험한 길" 배지가 쓴다. */
        val newRoadMeters: Double,
    )

    fun classify(points: List<LatLon>, matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M): Result {
        if (points.size < 2) {
            return Result(oldGeoJson = IosRouteGeoJson.empty, newGeoJson = IosRouteGeoJson.empty, newRoadMeters = 0.0)
        }
        val oldRuns = mutableListOf<List<LatLon>>()
        val newRuns = mutableListOf<List<LatLon>>()
        var currentRun = mutableListOf(points.first())
        var currentIsOld = isNearExploredSegment(points.first(), matchDistanceM)
        for (i in 1 until points.size) {
            val point = points[i]
            val isOld = isNearExploredSegment(point, matchDistanceM)
            if (isOld != currentIsOld) {
                // 색이 바뀌는 지점을 양쪽 구간이 공유해야 선이 안 끊겨 보인다.
                currentRun.add(point)
                (if (currentIsOld) oldRuns else newRuns).add(currentRun)
                currentRun = mutableListOf(point)
                currentIsOld = isOld
            } else {
                currentRun.add(point)
            }
        }
        if (currentRun.size >= 2) (if (currentIsOld) oldRuns else newRuns).add(currentRun)

        return Result(
            oldGeoJson = IosRouteGeoJson.lines(oldRuns),
            newGeoJson = IosRouteGeoJson.lines(newRuns),
            newRoadMeters = newRuns.sumOf { GeoMath.totalDistanceMeters(it) },
        )
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
