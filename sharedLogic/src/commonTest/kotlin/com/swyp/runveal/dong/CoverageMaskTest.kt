package com.swyp.runveal.dong

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverageMaskTest {

    private fun maskOf(size: Int, vararg covered: Int) = CoverageMask.empty(size).apply {
        covered.forEach { set(it) }
    }

    @Test
    fun `켠 비트만 덮인 것으로 센다`() {
        val mask = maskOf(10, 0, 1, 9)

        assertEquals(3, mask.coveredCount)
        assertEquals(0.3, mask.coveredRatio, absoluteTolerance = 1e-9)
        assertTrue(mask[9])
        assertFalse(mask[5])
        assertFalse(mask[10], "범위를 넘으면 false — 해상도가 달라졌을 때 터지면 안 된다")
    }

    @Test
    fun `인코딩하고 되읽어도 같다`() {
        val mask = maskOf(20, 0, 7, 8, 19)

        val restored = CoverageMask.decode(mask.encode())

        assertEquals(mask.size, restored.size)
        assertEquals(mask.coveredCount, restored.coveredCount)
        (0 until mask.size).forEach { assertEquals(mask[it], restored[it], "비트 $it") }
    }

    @Test
    fun `OR로 다른 날 달린 부분이 합쳐진다`() {
        // 구간의 앞쪽 절반을 어제, 뒤쪽 절반을 오늘 달린 상황 — 합치면 전체가 돼야 한다.
        val yesterday = maskOf(8, 0, 1, 2, 3)
        val today = maskOf(8, 4, 5, 6, 7)

        val merged = yesterday.or(today)

        assertTrue(merged.isFull())
        assertEquals(8, merged.coveredCount)
    }

    @Test
    fun `andNot으로 이번에 새로 덮인 부분만 남는다`() {
        val alreadyWalked = maskOf(8, 0, 1, 2, 3)
        val thisRun = maskOf(8, 2, 3, 4, 5)

        val added = thisRun.andNot(alreadyWalked)

        assertEquals(2, added.coveredCount)
        assertTrue(added[4] && added[5])
        assertFalse(added[2], "이미 가본 자리는 새 길이 아니다")
    }

    @Test
    fun `해상도가 바뀌어도 비율이 유지된다`() {
        // 지도 버전이 바뀌어 같은 구간의 샘플 수가 달라지는 경우.
        val half = maskOf(10, 0, 1, 2, 3, 4)

        val resized = half.resizedTo(20)

        assertEquals(20, resized.size)
        assertEquals(0.5, resized.coveredRatio, absoluteTolerance = 0.1)
    }

    @Test
    fun `전체를 덮은 기록은 해상도가 바뀌어도 전체로 남는다`() {
        assertTrue(CoverageMask.full(4).resizedTo(37).isFull(), "옛 적립분이 일부로 깎이면 탐험률이 줄어든다")
    }

    @Test
    fun `빈 문자열은 구간 전체로 읽는다`() {
        // 쪼개 적립하기 전에 쌓인 행(구간 전체 적립)이 그런 모양이라, 0으로 읽으면 이미 달린 길이
        // 갑자기 안 간 길이 된다.
        val legacy = CoverageMask.decode("")

        assertTrue(legacy.isFull())
        assertTrue(legacy.resizedTo(50).isFull())
    }

    @Test
    fun `형식이 깨진 값도 전체로 읽는다`() {
        assertTrue(CoverageMask.decode("쓰레기").isFull())
    }
}
