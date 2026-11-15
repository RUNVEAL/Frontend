package com.swyp.runveal.recording

import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon

/**
 * 추천 코스를 **실제로 지나간 만큼**만 진행률로 세는 추적기.
 *
 * "달린 거리 ÷ 코스 거리"로 세면 코스와 상관없는 곳을 달려도 진행률이 오르고, 코스를 다 안 돌았는데도
 * 거리만 채우면 100%가 된다. 그래서 코스 점을 앞에서부터 하나씩 "통과했다"고 확정해 나가는 방식으로 센다.
 *
 * 규칙 세 가지:
 * 1. **근접해야 인정** — 현위치가 코스 점에서 [onCourseRadiusMeters] 안에 들어와야 그 점을 통과로 친다.
 *    (코스 밖을 달리면 진행률이 그대로 멈춘다)
 * 2. **앞으로만** — 이미 확정한 지점보다 뒤로는 돌아가지 않는다. 진행률이 줄거나 요동치지 않는다.
 * 3. **멀리 건너뛰지 않음** — 확정 지점에서 코스를 따라 [lookAheadMeters] 안쪽만 살핀다.
 *    순환 코스는 출발점과 도착점이 같은 자리라, 이 창이 없으면 출발하자마자 마지막 점이 "가깝다"고
 *    잡혀서 진행률이 100%로 튀고 코스를 다 돈 것처럼 보인다 — 실제로 보고된 증상이 이것이다.
 *
 * 좌표만 받는 순수 로직이라 플랫폼과 무관하게 테스트할 수 있다.
 */
class CourseProgressTracker(
    private val coursePath: List<LatLon>,
    private val onCourseRadiusMeters: Double = DEFAULT_ON_COURSE_RADIUS_METERS,
    private val lookAheadMeters: Double = DEFAULT_LOOK_AHEAD_METERS,
) {
    /** 코스 시작점부터 각 점까지의 누적 거리(m). */
    private val cumulativeMeters: DoubleArray = DoubleArray(coursePath.size).also { cumulative ->
        for (i in 1..coursePath.lastIndex) {
            cumulative[i] = cumulative[i - 1] + GeoMath.distanceMeters(coursePath[i - 1], coursePath[i])
        }
    }

    /** 여기까지는 실제로 지나간 것으로 확정된 코스 점. */
    private var passedIndex: Int = 0

    val totalMeters: Double get() = cumulativeMeters.lastOrNull() ?: 0.0
    val passedMeters: Double get() = if (coursePath.isEmpty()) 0.0 else cumulativeMeters[passedIndex]
    val remainingMeters: Double get() = (totalMeters - passedMeters).coerceAtLeast(0.0)

    /** 코스 끝점까지 통과했을 때만 true — 거리만 채운 경우는 완료가 아니다. */
    val isComplete: Boolean get() = coursePath.size >= 2 && passedIndex == coursePath.lastIndex

    val progressPercent: Int
        get() = if (totalMeters <= 0.0) 0 else ((passedMeters / totalMeters) * 100).toInt().coerceIn(0, 100)

    /** 새로 들어온 위치들을 순서대로 반영한다. 이미 반영한 점을 다시 넣어도 결과는 같다(멱등). */
    fun advance(points: List<LatLon>) {
        points.forEach(::advance)
    }

    fun advance(point: LatLon) {
        if (coursePath.size < 2) return
        var furthest = passedIndex
        var index = passedIndex
        while (index <= coursePath.lastIndex &&
            cumulativeMeters[index] - cumulativeMeters[passedIndex] <= lookAheadMeters
        ) {
            if (GeoMath.distanceMeters(coursePath[index], point) <= onCourseRadiusMeters) furthest = index
            index++
        }
        passedIndex = furthest
    }

    companion object {
        /** GPS 오차와 보도/차도 폭을 감안한 "코스 위" 판정 반경. */
        const val DEFAULT_ON_COURSE_RADIUS_METERS = 30.0

        /** 한 번에 인정할 수 있는 최대 전진 거리. 코스를 따라 이만큼 앞까지만 살핀다. */
        const val DEFAULT_LOOK_AHEAD_METERS = 300.0
    }
}
