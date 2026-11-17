package com.sloth.runveal.dong

import kotlin.test.Test
import kotlin.test.assertEquals

class DongExploreRateTest {
    @Test
    fun `분모가 0이거나 없으면 0%`() {
        assertEquals(0, DongExploreRate.percentOf(walkedMeters = 1234.0, totalMeters = 0.0))
        assertEquals(
            emptyMap(),
            DongExploreRate.percents(
                walkedLengthsByDong = mapOf("4111566000" to 2936.0),
                totalLengthsByDong = emptyMap(),
            ),
        )
    }

    @Test
    fun `내림이라 조금 달린 동은 올라가지 않는다`() {
        // 2936 / 25959 = 11.3% -> 11
        assertEquals(11, DongExploreRate.percentOf(walkedMeters = 2936.0, totalMeters = 25959.0))
        // 3843 / 89196 = 4.3% -> 4 (RunvealDong 기준 1단계)
        assertEquals(4, DongExploreRate.percentOf(walkedMeters = 3843.0, totalMeters = 89196.0))
    }

    @Test
    fun `100%를 넘지 않는다`() {
        assertEquals(100, DongExploreRate.percentOf(walkedMeters = 50_000.0, totalMeters = 25_000.0))
    }

    @Test
    fun `0%인 동은 결과에서 빠진다`() {
        val percents = DongExploreRate.percents(
            walkedLengthsByDong = mapOf(
                "4111566000" to 2936.0,
                "4111573000" to 3843.0,
                "4420025000" to 1.0, // 분모 대비 0.x% — 내림하면 0이라 빠진다
            ),
            totalLengthsByDong = mapOf(
                "4111566000" to 25959.0,
                "4111573000" to 89196.0,
                "4420025000" to 10_000.0,
            ),
        )
        assertEquals(mapOf("4111566000" to 11, "4111573000" to 4), percents)
    }
}
