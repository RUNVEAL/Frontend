package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DongCoverageCalculatorTest {

    // 두 개의 겹치지 않는 정사각형 동.
    private val dongA = DongBoundary(
        admCd2 = "A",
        name = "A동",
        rings = listOf(listOf(
            LatLon(36.0, 127.0), LatLon(36.0, 127.1), LatLon(36.1, 127.1), LatLon(36.1, 127.0), LatLon(36.0, 127.0),
        )),
    )
    private val dongB = DongBoundary(
        admCd2 = "B",
        name = "B동",
        rings = listOf(listOf(
            LatLon(37.0, 128.0), LatLon(37.0, 128.1), LatLon(37.1, 128.1), LatLon(37.1, 128.0), LatLon(37.0, 128.0),
        )),
    )
    private val boundaries = listOf(dongA, dongB)

    @Test
    fun findsDongContainingPoint() {
        assertEquals("A", DongCoverageCalculator.findDong(boundaries, LatLon(36.05, 127.05)))
        assertEquals("B", DongCoverageCalculator.findDong(boundaries, LatLon(37.05, 128.05)))
    }

    @Test
    fun returnsNullForPointOutsideEveryDong() {
        assertNull(DongCoverageCalculator.findDong(boundaries, LatLon(0.0, 0.0)))
    }

    @Test
    fun sumsEdgeLengthIntoDongContainingSegmentMidpoint() {
        val edge = MatchedEdge(wayId = 1L, coordinates = listOf(LatLon(36.05, 127.0), LatLon(36.05, 127.01)))

        val totals = DongCoverageCalculator.walkedLengthByDong(boundaries, listOf(edge))

        assertEquals(1, totals.size)
        assertTrue(totals.getValue("A") > 0.0)
    }

    @Test
    fun dropsSegmentsWhoseMidpointFallsOutsideEveryDong() {
        val edge = MatchedEdge(wayId = 1L, coordinates = listOf(LatLon(0.0, 0.0), LatLon(0.0, 0.01)))

        val totals = DongCoverageCalculator.walkedLengthByDong(boundaries, listOf(edge))

        assertTrue(totals.isEmpty())
    }

    @Test
    fun countsEdgeWithSameWayIdOnlyOnceAcrossTracks() {
        val edge = MatchedEdge(wayId = 42L, coordinates = listOf(LatLon(36.05, 127.0), LatLon(36.05, 127.01)))

        val totals = DongCoverageCalculator.walkedLengthByDong(boundaries, listOf(edge, edge.copy()))

        val single = DongCoverageCalculator.walkedLengthByDong(boundaries, listOf(edge))
        assertEquals(single.getValue("A"), totals.getValue("A"), 0.001)
    }

    @Test
    fun edgesWithoutWayIdAreNeverDedupedAgainstEachOther() {
        val edge = MatchedEdge(wayId = null, coordinates = listOf(LatLon(36.05, 127.0), LatLon(36.05, 127.01)))

        val totals = DongCoverageCalculator.walkedLengthByDong(boundaries, listOf(edge, edge.copy()))

        val single = DongCoverageCalculator.walkedLengthByDong(boundaries, listOf(edge))
        assertEquals(single.getValue("A") * 2, totals.getValue("A"), 0.001)
    }
}
