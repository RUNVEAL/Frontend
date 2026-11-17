package com.sloth.runveal.dong

import com.sloth.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val METERS_PER_DEGREE_LAT = 111_320.0
private const val METERS_PER_DEGREE_LON = 88_316.0

private fun at(northM: Double, eastM: Double = 0.0) = LatLon(
    lat = 37.0 + northM / METERS_PER_DEGREE_LAT,
    lon = 127.0 + eastM / METERS_PER_DEGREE_LON,
)

/** 북쪽으로 뻗은 도로 한 줄(100m). [eastM]로 나란히 떨어뜨린다. */
private fun road(eastM: Double) = listOf(listOf(at(0.0, eastM), at(100.0, eastM)))

class SegmentCoverageMatcherTest {

    @Test
    fun `점에서 도로선까지의 최단거리를 잰다`() {
        val distance = SegmentCoverageMatcher.distanceToPathsMeters(at(50.0, 8.0), road(eastM = 0.0))

        assertEquals(8.0, distance, absoluteTolerance = 0.5)
    }

    @Test
    fun `선이 없으면 최대값을 돌려준다`() {
        assertEquals(Double.MAX_VALUE, SegmentCoverageMatcher.distanceToPathsMeters(at(0.0), emptyList()))
    }

    @Test
    fun `나란한 두 도로는 거리로 구분된다`() {
        // 실시간 분류가 "근처에 가본 구간이 있나" 대신 "가장 가까운 구간이 무엇인가"로 판단할 수 있어야
        // 하는 상황 — 지금 달리는 길(0m)과 12m 옆 다른 길이 둘 다 15m 반경 안에 들어온다.
        val point = at(50.0, 1.0)

        val onRoad = SegmentCoverageMatcher.distanceToPathsMeters(point, road(eastM = 0.0))
        val neighbor = SegmentCoverageMatcher.distanceToPathsMeters(point, road(eastM = 12.0))

        assertTrue(SegmentCoverageMatcher.isNearAnyPath(point, road(eastM = 12.0), matchDistanceM = 15.0))
        assertTrue(onRoad < neighbor, "밟고 있는 길이 더 가까워야 한다 (onRoad=$onRoad, neighbor=$neighbor)")
    }

    @Test
    fun `도로의 지나간 부분만 켜진다`() {
        // 100m 도로의 앞 40m만 달린 상황 — 구간 전체를 통째로 인정하면 60m를 안 가고 적립한다.
        val samples = SegmentCoverageMatcher.samplePoints(road(eastM = 0.0))
        val ranPath = listOf(listOf(at(0.0), at(40.0)))

        val mask = SegmentCoverageMatcher.coverageMask(samples, ranPath, matchDistanceM = 15.0)

        assertTrue(mask[0], "출발 지점은 지나갔다")
        assertFalse(mask[samples.lastIndex], "100m 지점은 안 갔다")
        assertTrue(mask.coveredRatio < 0.7, "구간 전체가 적립되면 안 된다 (ratio=${mask.coveredRatio})")
        assertTrue(mask.coveredRatio > 0.35, "달린 만큼은 적립돼야 한다 (ratio=${mask.coveredRatio})")
    }

    @Test
    fun `지나간 부분의 선은 떨어진 조각을 잇지 않는다`() {
        // MultiLineString 구간(떨어진 두 조각)을 전부 지난 경우 — 조각 사이를 직선으로 이으면 안 지난
        // 곳에 금색 선이 생긴다.
        val sampleLines = SegmentCoverageMatcher.samplePointsByLine(
            listOf(listOf(at(0.0), at(30.0)), listOf(at(200.0), at(230.0))),
        )
        val full = CoverageMask.full(sampleLines.sumOf { it.size })

        val paths = SegmentCoverageMatcher.coveredSubPaths(sampleLines, full)

        assertEquals(2, paths.size)
    }

    @Test
    fun `지나간 부분의 선은 끊긴 자리에서 토막난다`() {
        val sampleLines = SegmentCoverageMatcher.samplePointsByLine(road(eastM = 0.0))
        val mask = CoverageMask.empty(sampleLines.first().size)
        mask.set(0)
        mask.set(1)
        mask.set(5)
        mask.set(6)

        val paths = SegmentCoverageMatcher.coveredSubPaths(sampleLines, mask)

        assertEquals(2, paths.size)
        assertEquals(2, paths.first().size)
    }
}
