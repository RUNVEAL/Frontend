package com.sloth.runveal.recording

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrackFilterTest {

    private fun point(lat: Double, lon: Double, accuracy: Float = 5f, speed: Float = 2f, time: Long = 0L) =
        TrackPoint(lat = lat, lon = lon, accuracyMeters = accuracy, speedMps = speed, timeEpochMs = time)

    @Test
    fun firstPointPassesWithNoLastKept() {
        val result = TrackFilter.evaluate(point(37.5665, 126.9780), lastKept = null)
        assertNull(result)
    }

    @Test
    fun dropsWhenAccuracyWorseThan20Meters() {
        val result = TrackFilter.evaluate(point(37.5665, 126.9780, accuracy = 20.1f), lastKept = null)
        assertEquals(DropReason.ACCURACY, result)
    }

    @Test
    fun acceptsExactly20MetersAccuracy() {
        val result = TrackFilter.evaluate(point(37.5665, 126.9780, accuracy = 20f), lastKept = null)
        assertNull(result)
    }

    @Test
    fun dropsWhenSpeedExceeds8Mps() {
        val result = TrackFilter.evaluate(point(37.5665, 126.9780, speed = 8.1f), lastKept = null)
        assertEquals(DropReason.SPEED, result)
    }

    @Test
    fun dropsWhenCloserThan3MetersToLastKept() {
        val last = point(37.5665, 126.9780)
        // 위도로 약 0.00001도 이동 ≈ 1.1m — 3m 미만.
        val next = point(37.56651, 126.9780)
        assertEquals(DropReason.DISTANCE, TrackFilter.evaluate(next, last))
    }

    @Test
    fun keepsWhenFartherThan3MetersFromLastKept() {
        val last = point(37.5665, 126.9780)
        // 위도로 약 0.0001도 이동 ≈ 11m — 3m 이상.
        val next = point(37.5666, 126.9780)
        assertNull(TrackFilter.evaluate(next, last))
    }
}
