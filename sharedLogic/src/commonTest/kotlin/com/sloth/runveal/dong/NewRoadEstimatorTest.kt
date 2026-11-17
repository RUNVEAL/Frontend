package com.sloth.runveal.dong

import kotlin.test.Test
import kotlin.test.assertEquals

class NewRoadEstimatorTest {

    @Test
    fun subtractsExploredLengthFromTotal() {
        assertEquals(3000.0, NewRoadEstimator.newRoadMeters(totalDistanceM = 5000.0, exploredLengthM = 2000.0))
    }

    @Test
    fun treatsNothingExploredAsAllNewRoad() {
        assertEquals(5000.0, NewRoadEstimator.newRoadMeters(totalDistanceM = 5000.0, exploredLengthM = 0.0))
    }

    @Test
    fun clampsToZeroWhenExploredLengthExceedsTotal() {
        // 코스가 구간 일부만 지나가도 explored_segment엔 그 구간 전체 길이가 들어 있어서, 합이 코스
        // 길이를 넘을 수 있다 — 음수(-500m) 대신 0으로 자른다.
        assertEquals(0.0, NewRoadEstimator.newRoadMeters(totalDistanceM = 5000.0, exploredLengthM = 5500.0))
    }

    @Test
    fun neverReportsMoreNewRoadThanTheCourseItself() {
        assertEquals(5000.0, NewRoadEstimator.newRoadMeters(totalDistanceM = 5000.0, exploredLengthM = -10.0))
    }
}
