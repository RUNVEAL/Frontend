package com.swyp.runveal.match

import com.swyp.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val METERS_PER_DEGREE_LAT = 111_320.0

private fun north(meters: Double, offsetEastM: Double = 0.0) = LatLon(
    lat = 37.0 + meters / METERS_PER_DEGREE_LAT,
    lon = 127.0 + offsetEastM / 88_316.0,
)

/** 북쪽으로 [lengthM]만큼, 10m 간격으로 찍힌 점들. */
private fun line(lengthM: Double, offsetEastM: Double = 0.0, startM: Double = 0.0): List<LatLon> =
    (0..((lengthM) / 10).toInt()).map { north(startM + it * 10.0, offsetEastM) }

class MatchedPathFilterTest {

    @Test
    fun `GPS와 나란히 붙어 있는 매칭 경로는 그대로 둔다`() {
        val recorded = line(500.0)
        // 도로에 붙느라 10m 옆으로 밀린 정상 매칭.
        val matched = line(500.0, offsetEastM = 10.0)

        val runs = MatchedPathFilter.keepNearRecordedPoints(matched, recorded)

        assertEquals(1, runs.size)
        assertEquals(matched.size, runs.first().size)
    }

    @Test
    fun `도로 데이터가 없는 구간을 메운 경로는 잘라낸다`() {
        // 실제로는 0~200m만 달렸는데, 서버가 200~800m 구간을 엉뚱한 도로로 메워서 돌려준 상황.
        val recorded = line(200.0)
        val matched = line(200.0, offsetEastM = 5.0) + line(600.0, offsetEastM = 300.0, startM = 200.0)

        val runs = MatchedPathFilter.keepNearRecordedPoints(matched, recorded)

        assertEquals(1, runs.size)
        assertTrue(runs.first().all { it.lon < north(0.0, offsetEastM = 50.0).lon }, "메운 구간이 남아 있다")
    }

    @Test
    fun `중간만 벗어나면 선을 둘로 나누고 이어 붙이지 않는다`() {
        // 앞뒤 100m는 도로에 붙었고, 가운데 300m는 도로가 없어 엉뚱하게 메워진 경우.
        val recorded = line(100.0) + line(100.0, startM = 400.0)
        val matched = line(100.0, offsetEastM = 5.0) +
            line(300.0, offsetEastM = 400.0, startM = 100.0) +
            line(100.0, offsetEastM = 5.0, startM = 400.0)

        val runs = MatchedPathFilter.keepNearRecordedPoints(matched, recorded)

        assertEquals(2, runs.size, "메운 구간을 빼고 앞뒤 두 조각이 남아야 한다")
        assertTrue(runs.all { run -> run.all { it.lon < north(0.0, offsetEastM = 50.0).lon } })
    }

    @Test
    fun `남는 점이 하나뿐인 조각은 버린다`() {
        val recorded = listOf(north(0.0))
        val matched = listOf(north(0.0, 5.0), north(500.0, 500.0), north(1000.0, 500.0))

        val runs = MatchedPathFilter.keepNearRecordedPoints(matched, recorded)

        assertEquals(emptyList(), runs)
    }

    @Test
    fun `원본 GPS가 비어 있으면 비교할 기준이 없어 그대로 둔다`() {
        val matched = line(300.0)

        val runs = MatchedPathFilter.keepNearRecordedPoints(matched, recordedPoints = emptyList())

        assertEquals(listOf(matched), runs)
    }
}
