package com.swyp.runveal.running.contract

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.ui.mvi.UiState

enum class RecordingPhase { IDLE, RECORDING, PAUSED }

/** `elapsedLabel`/`distanceLabel`/`paceLabel`은 이미 포맷된 문자열로 들고 있는다(화면은 그대로 쓰기만
 * 한다). [distanceLabel]은 단위(km) 없이 숫자만 — 디자인이 숫자/단위를 다른 스타일로 그리기 때문.
 * [selectedCoursePath]는 "이 코스로 러닝 시작"으로 넘어온 코스 경로 — 지금은 보관만 하고 지도
 * 가이드/오버레이로 그리는 건 보류. */
data class RecordingState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val elapsedLabel: String = "00:00",
    val distanceLabel: String = "0.00",
    val paceLabel: String = "0'00″",
    val selectedCoursePath: List<LatLon>? = null,
) : UiState
