package com.swyp.runveal.dong.glow

import com.swyp.runveal.map.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class MercatorTest {

    @Test
    fun roundTripsWithinOneBillionthDegree() {
        val samples = listOf(
            LatLon(lat = 37.5665, lon = 126.9780), // 서울시청
            LatLon(lat = 36.7898, lon = 127.0040), // 아산
            LatLon(lat = 0.0, lon = 0.0),
            LatLon(lat = -33.8688, lon = 151.2093), // 시드니(남반구 확인용)
            LatLon(lat = 63.0, lon = -19.0), // 고위도
        )
        for (point in samples) {
            val roundTrip = Mercator.unproject(Mercator.project(point))
            assertTrue(abs(roundTrip.lat - point.lat) < 1e-9, "lat diff too large for $point: $roundTrip")
            assertTrue(abs(roundTrip.lon - point.lon) < 1e-9, "lon diff too large for $point: $roundTrip")
        }
    }
}
