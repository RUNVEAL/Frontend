package com.swyp.runveal.result

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 러닝 중 "지금까지 새로 탐험한 길" 길이(m). 지도(`HomeScreen`)가 1초마다 `LiveRoadClassifier`로
 * 오늘 달린 선을 가본 구간/처음 가보는 구간으로 나눠 금색·흰색으로 칠하는데, 그때 나온 새 구간
 * 길이를 그대로 [publish]한다 — 기록 화면 배지가 같은 값을 읽어서, 지도에서 금색으로 보이는 길과
 * 배지 숫자가 항상 같은 근거를 쓴다.
 *
 * 분류는 전국 구간 인덱스를 훑는 작업이라 화면마다 따로 돌리면 매초 같은 계산을 두 번 하게 된다 —
 * 그래서 계산은 지도 한 곳에서만 하고 결과만 여기로 넘긴다. 러닝을 시작·종료할 때 `TrackingService`가
 * [clear]로 지난 러닝 값을 지운다.
 *
 * 서버 매칭이 아니라 로컬 판정이라(`docs/api/API.md` §3.5) 종료 후 최종 매칭(§6)으로 계산하는
 * 결과 화면 값과는 조금 다를 수 있다 — 달리는 동안 보여주는 어림값이다.
 */
object LiveNewRoadHolder {
    private val _state = MutableStateFlow(0.0)
    val state: StateFlow<Double> = _state.asStateFlow()

    fun publish(newRoadMeters: Double) {
        _state.value = newRoadMeters
    }

    fun clear() {
        _state.value = 0.0
    }
}
