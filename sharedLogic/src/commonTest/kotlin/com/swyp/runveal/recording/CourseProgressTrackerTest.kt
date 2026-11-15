package com.swyp.runveal.recording

import com.swyp.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 위도 1도 ≈ 111.32km — 테스트 좌표를 m 단위로 만들기 위한 환산값. */
private const val METERS_PER_DEGREE_LAT = 111_320.0

private fun north(meters: Double) = LatLon(lat = 37.0 + meters / METERS_PER_DEGREE_LAT, lon = 127.0)

/** 북쪽으로 [lengthM]만큼 뻗은 직선 코스(10m 간격). */
private fun straightCourse(lengthM: Double): List<LatLon> =
    (0..(lengthM / 10).toInt()).map { north(it * 10.0) }

/** 북쪽으로 갔다가 같은 길로 되돌아오는 순환 코스 — 출발점과 도착점이 같은 자리다. */
private fun loopCourse(halfLengthM: Double): List<LatLon> =
    straightCourse(halfLengthM) + straightCourse(halfLengthM).reversed().drop(1)

/**
 * 판정 반경(기본 30m) 안의 점은 "지나갔다"고 보므로 진행률은 코스 거리 기준으로 반경만큼 앞서 나갈 수
 * 있다. 테스트는 그 오차를 감안해 범위로 본다 — 중요한 건 **코스를 안 지나면 안 오르고, 끝까지 지나야
 * 100%가 된다**는 성질이다.
 */
class CourseProgressTrackerTest {

    @Test
    fun `순환 코스 출발점에서는 진행률이 0이다`() {
        // 출발점 = 도착점이라, 전방 탐색 창이 없으면 마지막 점이 "가깝다"고 잡혀 100%로 튄다.
        val tracker = CourseProgressTracker(loopCourse(halfLengthM = 1000.0))

        tracker.advance(north(0.0))

        assertTrue(tracker.progressPercent <= 2, "출발점인데 ${tracker.progressPercent}%")
        assertFalse(tracker.isComplete)
    }

    @Test
    fun `코스 밖에서 달리면 진행률이 오르지 않는다`() {
        val tracker = CourseProgressTracker(straightCourse(lengthM = 1000.0))

        // 코스와 나란하지만 200m 떨어진 길을 따라 이동.
        (0..100).forEach { step ->
            tracker.advance(LatLon(lat = north(step * 10.0).lat, lon = 127.0 + 200.0 / 88_316.0))
        }

        assertEquals(0, tracker.progressPercent)
        assertEquals(tracker.totalMeters, tracker.remainingMeters, absoluteTolerance = 0.5)
    }

    @Test
    fun `코스를 따라 절반 지나면 절반만 센다`() {
        val tracker = CourseProgressTracker(straightCourse(lengthM = 1000.0))

        (0..50).forEach { step -> tracker.advance(north(step * 10.0)) }

        assertTrue(tracker.progressPercent in 48..55, "절반 지점인데 ${tracker.progressPercent}%")
        assertFalse(tracker.isComplete)
    }

    @Test
    fun `중간을 건너뛰면 건너뛴 구간은 인정되지 않는다`() {
        val tracker = CourseProgressTracker(straightCourse(lengthM = 2000.0))

        // 200m까지 따라가다가 차를 타고 1800m 지점으로 점프한 상황.
        (0..20).forEach { step -> tracker.advance(north(step * 10.0)) }
        tracker.advance(north(1800.0))

        // 200m / 2000m = 10% 언저리. 점프한 1800m 지점은 전방 창(300m) 밖이라 인정되지 않는다.
        assertTrue(tracker.progressPercent in 9..13, "건너뛰었는데 ${tracker.progressPercent}%")
    }

    @Test
    fun `코스를 끝까지 지나야 완료다`() {
        val course = straightCourse(lengthM = 500.0)
        val tracker = CourseProgressTracker(course)

        // 끝에서 50m 못 미친 지점까지만 지나간 상태 — 판정 반경(30m) 밖이라 아직 완료가 아니다.
        course.dropLast(5).forEach(tracker::advance)
        assertFalse(tracker.isComplete)

        tracker.advance(course.last())
        assertTrue(tracker.isComplete)
        assertEquals(100, tracker.progressPercent)
        assertEquals(0.0, tracker.remainingMeters, absoluteTolerance = 0.5)
    }

    @Test
    fun `진행률은 뒤로 가지 않는다`() {
        val tracker = CourseProgressTracker(straightCourse(lengthM = 1000.0))

        (0..50).forEach { step -> tracker.advance(north(step * 10.0)) }
        val halfway = tracker.progressPercent
        // 왔던 길을 되돌아가도 진행률은 유지된다.
        (50 downTo 0).forEach { step -> tracker.advance(north(step * 10.0)) }

        assertEquals(halfway, tracker.progressPercent)
    }
}
