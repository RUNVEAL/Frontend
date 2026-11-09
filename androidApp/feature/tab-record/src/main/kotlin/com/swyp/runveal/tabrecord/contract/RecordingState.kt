package com.swyp.runveal.tabrecord.contract

import com.swyp.runveal.ui.mvi.UiState

enum class RecordingPhase { IDLE, RECORDING, PAUSED }

/** `elapsedLabel`/`distanceLabel`/`paceLabel`은 이미 포맷된 문자열로 들고 있는다(화면은 그대로 쓰기만
 * 한다). [distanceLabel]은 단위(km) 없이 숫자만 — 디자인이 숫자/단위를 다른 스타일로 그리기 때문. */
data class RecordingState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val elapsedLabel: String = "00:00",
    val distanceLabel: String = "0.00",
    val paceLabel: String = "0'00″",
) : UiState
