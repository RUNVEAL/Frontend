package com.swyp.runveal.map.snapshot

import com.swyp.runveal.map.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnapshotBoundsTest {

    @Test
    fun appliesPaddingRatioToBbox() {
        // 위도 0~1도, 경도 0~1도짜리 정사각 bbox. 패딩 12%가 양쪽에 똑같이 붙어야 한다.
        val bounds = listOf(LatLon(0.0, 0.0), LatLon(1.0, 1.0)).snapshotBounds()

        assertApproxEquals(-0.12, bounds.south)
        assertApproxEquals(1.12, bounds.north)
        assertApproxEquals(-0.12, bounds.west)
        assertApproxEquals(1.12, bounds.east)
    }

    @Test
    fun expandsToMinimumSpanForASinglePoint() {
        // 제자리 러닝(점 1개) — bbox span이 0이라 패딩만으로는 범위가 안 생긴다. 최소 300m가 적용돼야 한다.
        val bounds = listOf(LatLon(37.5665, 126.9780)).snapshotBounds()

        val latSpanMeters = (bounds.north - bounds.south) * 111_320.0
        assertTrue(abs(latSpanMeters - SnapshotConfig.MIN_SPAN_METERS) < 1.0)
    }

    @Test
    fun minimumSpanCorrectsLongitudeByLatitude() {
        // 위도가 높을수록(극에 가까울수록) 경도 1도의 실제 거리가 짧아진다 — 같은 미터 범위를 만들려면
        // 경도 쪽 degree span이 더 커야 한다. 적도 근처(서울, 위도 37.5)에서 확인한다.
        val equatorish = listOf(LatLon(0.0, 0.0)).snapshotBounds()
        val seoul = listOf(LatLon(37.5665, 126.9780)).snapshotBounds()

        val equatorLonSpanDegrees = equatorish.east - equatorish.west
        val seoulLonSpanDegrees = seoul.east - seoul.west
        assertTrue(seoulLonSpanDegrees > equatorLonSpanDegrees)
    }

    @Test
    fun boundsAreNotShrunkWhenRouteAlreadyExceedsMinimumSpan() {
        // 변이 1도(~111km)인 경로는 이미 최소 범위(300m)를 훌쩍 넘으므로 패딩 적용 후 값 그대로여야 한다.
        val bounds = listOf(LatLon(37.0, 127.0), LatLon(38.0, 128.0)).snapshotBounds()

        assertApproxEquals(36.88, bounds.south)
        assertApproxEquals(38.12, bounds.north)
    }

    @Test
    fun toSnapshotRequestReturnsNullForSingleOrEmptyRoute() {
        assertNull(listOf<LatLon>().toSnapshotRequest("id", SnapshotVariant.THUMBNAIL))
        assertNull(listOf(LatLon(37.0, 127.0)).toSnapshotRequest("id", SnapshotVariant.THUMBNAIL))
    }

    @Test
    fun toSnapshotRequestSetsStartEndOnlyForResultCard() {
        val route = listOf(LatLon(37.0, 127.0), LatLon(37.001, 127.001))

        val thumbnail = route.toSnapshotRequest("id", SnapshotVariant.THUMBNAIL)
        assertNotNull(thumbnail)
        assertNull(thumbnail.start)
        assertNull(thumbnail.end)

        val resultCard = route.toSnapshotRequest("id", SnapshotVariant.RESULT_CARD)
        assertNotNull(resultCard)
        assertEquals(route.first(), resultCard.start)
        assertEquals(route.last(), resultCard.end)
    }

    @Test
    fun highlightsNewRoadForRunRoutesInBothVariants() {
        val route = listOf(LatLon(37.0, 127.0), LatLon(37.001, 127.001), LatLon(37.002, 127.002))
        val newRoad = listOf(listOf(LatLon(37.001, 127.001), LatLon(37.002, 127.002)))

        // 결과 카드든 기록 썸네일이든 러닝이면 처음 간 길을 금색으로 덧그린다.
        listOf(SnapshotVariant.RESULT_CARD, SnapshotVariant.THUMBNAIL).forEach { variant ->
            val request = assertNotNull(route.toSnapshotRequest("id", variant, newRoad))
            assertTrue(request.isRunRoute)
            val highlight = assertNotNull(request.highlightGeoJson, "variant=$variant")
            assertTrue("MultiLineString" in highlight, highlight)
            assertTrue("127.002" in highlight, highlight)
        }
    }

    @Test
    fun treatsNullNewRoadPathsAsCourseAndDrawsOneColor() {
        val route = listOf(LatLon(37.0, 127.0), LatLon(37.001, 127.001))

        // 아직 달리지 않은 코스 — 넘기는 값이 없으면 러닝이 아니다.
        val course = assertNotNull(route.toSnapshotRequest("id", SnapshotVariant.THUMBNAIL))
        assertEquals(false, course.isRunRoute)
        assertNull(course.highlightGeoJson)
    }

    @Test
    fun keepsRunRouteEvenWhenNothingIsNewRoad() {
        val route = listOf(LatLon(37.0, 127.0), LatLon(37.001, 127.001))

        // 같은 길을 또 달린 러닝 — 금색은 없지만 경로는 "오늘 달린 길"(흰색)로 그려야 한다.
        val rerun = assertNotNull(route.toSnapshotRequest("id", SnapshotVariant.RESULT_CARD, emptyList()))
        assertTrue(rerun.isRunRoute)
        assertNull(rerun.highlightGeoJson)

        // 점이 1개뿐인 줄은 선이 못 되니 버린다.
        val degenerate = listOf(listOf(LatLon(37.0, 127.0)))
        assertNull(route.toSnapshotRequest("id", SnapshotVariant.RESULT_CARD, degenerate)?.highlightGeoJson)
    }

    private fun assertApproxEquals(expected: Double, actual: Double, tolerance: Double = 1e-9) {
        assertTrue(abs(expected - actual) < tolerance, "expected=$expected actual=$actual")
    }
}
