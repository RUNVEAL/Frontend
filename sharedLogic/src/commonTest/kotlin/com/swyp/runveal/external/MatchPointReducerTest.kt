package com.swyp.runveal.external

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchPointReducerTest {

    /** 1초 간격 점 — 외부 기록에서 흔한 밀도. */
    private fun route(count: Int, intervalMs: Long = 1_000L, startEpochMs: Long = 0L) =
        (0 until count).map { index ->
            ExternalWorkoutPoint(
                lat = 37.55 + index * 0.0001,
                lon = 126.92 + index * 0.0001,
                timeEpochMs = startEpochMs + index * intervalMs,
            )
        }

    @Test
    fun `점이 최소 개수보다 적으면 비운다`() {
        val result = MatchPointReducer.reduce(route(count = MatchPointReducer.MIN_POINTS - 1))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `빈 경로는 비운다`() {
        assertTrue(MatchPointReducer.reduce(emptyList()).isEmpty())
    }

    @Test
    fun `10초 간격으로 줄인다`() {
        // 1초 간격 601점 = 10분. 10초 간격이면 61점 근처로 줄어든다.
        val result = MatchPointReducer.reduce(route(count = 601))
        assertTrue(result.size in 50..65, "기대: 50~65점, 실제: ${result.size}")

        // 남은 점들의 간격이 모두 10초 이상인지 (마지막 점은 항상 포함되므로 예외일 수 있다)
        val gaps = result.zipWithNext { a, b -> b.timeEpochMs - a.timeEpochMs }
        assertTrue(gaps.dropLast(1).all { it >= 10_000L }, "10초 미만 간격이 남았다: $gaps")
    }

    @Test
    fun `양끝을 잘라낸다`() {
        // 1초 간격 600점(10분) — 앞뒤 15초는 빠져야 한다.
        val source = route(count = 600)
        val result = MatchPointReducer.reduce(source)

        val first = result.first().timeEpochMs
        val last = result.last().timeEpochMs
        assertTrue(first >= 15_000L, "앞쪽이 안 잘렸다: $first")
        assertTrue(last <= source.last().timeEpochMs - 15_000L, "뒤쪽이 안 잘렸다: $last")
    }

    @Test
    fun `짧은 기록은 자르지 않고 살린다`() {
        // 1초 간격 20점(19초) — 양끝 15초씩 자르면 아무것도 안 남는다. 그래도 최소 개수는 채워야 한다.
        val result = MatchPointReducer.reduce(route(count = 20))
        assertTrue(result.size >= MatchPointReducer.MIN_POINTS, "짧은 기록이 버려졌다: ${result.size}")
    }

    @Test
    fun `간격이 넓은 기록은 추가로 걸러내지 않는다`() {
        // 이미 30초 간격이면 10초 축약이 더 덜어낼 게 없다 — 양끝만 잘리고 중간 간격은 그대로다.
        val source = route(count = 20, intervalMs = 30_000L)
        val result = MatchPointReducer.reduce(source)

        val gaps = result.zipWithNext { a, b -> b.timeEpochMs - a.timeEpochMs }
        assertTrue(gaps.all { it == 30_000L }, "중간 점이 걸러졌다: $gaps")
        assertEquals(source.size - 2, result.size, "양끝 한 점씩만 잘려야 한다")
    }

    @Test
    fun `상한을 넘으면 최대 개수로 맞춘다`() {
        // 10초 간격 3000점 — 축약으로도 2000점을 넘으므로 더 걸러내야 한다.
        val result = MatchPointReducer.reduce(route(count = 3000, intervalMs = 10_000L))
        assertTrue(result.size <= MatchPointReducer.MAX_POINTS, "상한 초과: ${result.size}")
        assertEquals(MatchPointReducer.MAX_POINTS, result.size)
    }

    @Test
    fun `시간순이 아닌 입력도 정렬해서 처리한다`() {
        val shuffled = route(count = 600).shuffled()
        val result = MatchPointReducer.reduce(shuffled)
        val times = result.map { it.timeEpochMs }
        assertEquals(times.sorted(), times, "결과가 시간순이 아니다")
    }
}
