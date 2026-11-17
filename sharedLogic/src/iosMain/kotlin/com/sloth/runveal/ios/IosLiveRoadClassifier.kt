package com.sloth.runveal.ios

import com.sloth.runveal.data.local.ExploredCoverageIndex
import com.sloth.runveal.data.local.RoadSegmentLocator
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.db.RunvealDatabase
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon

/**
 * 러닝 중 "오늘 달린 길"을 이미 가본 구간(흰색)/처음 가보는 구간(금색)으로 나눈다 — 서버 호출 없이
 * 전부 로컬 판정이다(`docs/api/API.md` §3.5). Android `core:map`의 `LiveRoadClassifier`와 같은 로직이고,
 * 지도에 바로 넣을 GeoJSON까지 만들어 주는 것만 다르다.
 *
 * `road_segment` 인덱스가 비어 있으면(아직 [IosRoadSegmentIndexImporter]를 안 돌렸으면) 아무 점도
 * "가본 구간"으로 못 찾아서 전부 새 구간으로 본다 — 크래시 대신 "전부 새 길"로 보이는 쪽을 택한다.
 */
class IosLiveRoadClassifier(
    database: RunvealDatabase = RunvealDatabaseProvider.database,
) {
    private val locator = RoadSegmentLocator(database)
    private val explored = ExploredCoverageIndex(database)

    /**
     * "이미 가본 길" 적립 내역을 DB에서 다시 읽는다 — **러닝을 시작할 때마다** 불러야 한다.
     *
     * 안 부르면 앱을 켠 채 두 번째로 달릴 때 방금 달린 길이 또 처음 가는 길(금색)로 보인다 —
     * 예전에 적립 내역을 `by lazy`로 한 번만 읽어서 생겼던 문제다.
     */
    fun refreshExploredSegments() {
        explored.refresh()
    }

    /** 색이 바뀌는 지점마다 쪼갠 연속 구간 목록 — Android `LiveRoadClassifier.Result`에 해당. */
    data class Runs(
        val oldRuns: List<List<LatLon>>,
        val newRuns: List<List<LatLon>>,
    ) {
        /** 처음 가보는 구간들의 길이 합(m). 지도에 금색으로 칠하는 선과 같은 근거다. */
        val newRoadMeters: Double get() = newRuns.sumOf { GeoMath.totalDistanceMeters(it) }
    }

    /** 지도 레이어에 바로 넣을 수 있게 GeoJSON까지 만든 분류 결과. */
    data class Result(
        /** 이미 가본 구간(흰색) 선 레이어용 GeoJSON. */
        val oldGeoJson: String,
        /** 처음 가보는 구간(금색) 선 레이어용 GeoJSON. */
        val newGeoJson: String,
        /** 처음 가보는 구간들의 길이 합(m) — 기록 화면 "새로 탐험한 길" 배지가 쓴다. */
        val newRoadMeters: Double,
    )

    /**
     * 매칭 경로는 GPS와 동떨어진 부분을 잘라낸 **조각 목록**이라(`MatchedPathFilter`), 조각마다 따로
     * 분류해서 합친다 — 하나로 이어 붙이면 잘라낸 구간이 직선으로 되살아난다.
     */
    fun classifyPaths(paths: List<List<LatLon>>): Runs {
        val oldRuns = mutableListOf<List<LatLon>>()
        val newRuns = mutableListOf<List<LatLon>>()
        paths.forEach { path ->
            val runs = classify(path)
            oldRuns += runs.oldRuns
            newRuns += runs.newRuns
        }
        return Runs(oldRuns = oldRuns, newRuns = newRuns)
    }

    /** [classifyPaths]의 결과를 지도 레이어용 GeoJSON으로 묶은 것. */
    fun classifyPathsToGeoJson(paths: List<List<LatLon>>): Result {
        val runs = classifyPaths(paths)
        return Result(
            oldGeoJson = IosRouteGeoJson.lines(runs.oldRuns),
            newGeoJson = IosRouteGeoJson.lines(runs.newRuns),
            newRoadMeters = runs.newRoadMeters,
        )
    }

    /** [points]를 순서대로 훑어 "이미 가본 구간 위"인지가 바뀌는 지점마다 쪼갠다. */
    fun classify(points: List<LatLon>): Runs {
        if (points.size < 2) return Runs(emptyList(), emptyList())
        val oldRuns = mutableListOf<List<LatLon>>()
        val newRuns = mutableListOf<List<LatLon>>()
        var currentRun = mutableListOf(points.first())
        var currentIsOld = isOnExploredSegment(points.first())
        for (i in 1 until points.size) {
            val point = points[i]
            val isOld = isOnExploredSegment(point)
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
        return Runs(oldRuns = oldRuns, newRuns = newRuns)
    }

    /**
     * 이 점이 **지금 밟고 있는 구간**이 이미 가본 구간인지.
     *
     * "반경 안에 가본 구간이 하나라도 있으면 가본 길"로 보면 왕복 차로 반대편·나란한 이면도로·인도처럼
     * **다른 구간이 가까이 있기만 해도** 지금 처음 달리는 길이 흰색으로 칠해진다. 그래서 가본 구간만
     * 보지 말고 후보 전체에서 **가장 가까운 구간**을 고른다([RoadSegmentLocator]).
     *
     * 구간을 찾은 뒤 한 번 더 본다 — 그 구간의 **이 자리**를 지나갔는지. 적립이 구간 전체가 아니라
     * 샘플 단위라, 같은 구간이라도 안 밟은 쪽은 아직 새 길이다.
     */
    private fun isOnExploredSegment(point: LatLon): Boolean {
        if (explored.isEmpty()) return false
        val hit = locator.locate(point) ?: return false
        return explored.isCovered(hit.segmentId, hit.sampleIndex, hit.sampleCount)
    }
}
