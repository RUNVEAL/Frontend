package com.swyp.runveal.result

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 러닝 결과 화면의 "동네 탐험률" 한 줄 — 이번 트랙 기록 전/후 완주율(%). */
data class DongRateChange(
    val admCd2: String,
    val name: String,
    val beforePercent: Int,
    val afterPercent: Int,
)

/** 러닝 결과 화면이 그대로 쓰는 최종 데이터 — 최종 매칭(§6)이 끝나야 만들어진다. */
data class TrackResult(
    val trackId: String,
    val startedAtEpochMs: Long,
    val distanceMeters: Double,
    val elapsedMs: Long,
    val newRoadMeters: Double,
    val dongStats: List<DongRateChange>,
)

/**
 * 기록 종료 → 최종 매칭 완료 사이의 간극을 메우는 StateFlow 싱글톤. `TrackingService`(androidMain)가
 * 매칭이 끝나는 대로 [publish]하고, 결과 화면이 이 값이 채워질 때까지 로딩 상태로 기다린다.
 */
object TrackResultHolder {
    private val _state = MutableStateFlow<TrackResult?>(null)
    val state: StateFlow<TrackResult?> = _state.asStateFlow()

    fun publish(result: TrackResult) {
        _state.value = result
    }

    fun clear() {
        _state.value = null
    }
}
