package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RingWindingTest {

    // 반시계(CCW): (0,0) -> (1,0) -> (1,1) -> (0,1) -> close. lon=x, lat=y로 보면 신 발끈 면적이 양수.
    private val ccwSquare = listOf(
        LatLon(lat = 0.0, lon = 0.0), LatLon(lat = 0.0, lon = 1.0),
        LatLon(lat = 1.0, lon = 1.0), LatLon(lat = 1.0, lon = 0.0),
    )
    private val cwSquare = ccwSquare.reversed()

    @Test
    fun ccwRingIsCounterClockwise() {
        assertTrue(RingWinding.isCounterClockwise(ccwSquare))
        assertTrue(RingWinding.signedArea(ccwSquare) > 0)
    }

    @Test
    fun cwRingIsNotCounterClockwise() {
        assertTrue(!RingWinding.isCounterClockwise(cwSquare))
        assertTrue(RingWinding.signedArea(cwSquare) < 0)
    }

    @Test
    fun normalizeKeepsAlreadyCorrectWinding() {
        val result = RingWinding.normalize(ccwSquare, holes = listOf(cwSquare))
        assertEquals(ccwSquare, result.exterior)
        assertEquals(cwSquare, result.holes.first())
    }

    @Test
    fun normalizeFlipsReversedWinding() {
        val result = RingWinding.normalize(cwSquare, holes = listOf(ccwSquare))
        assertTrue(RingWinding.isCounterClockwise(result.exterior))
        assertTrue(!RingWinding.isCounterClockwise(result.holes.first()))
    }
}
