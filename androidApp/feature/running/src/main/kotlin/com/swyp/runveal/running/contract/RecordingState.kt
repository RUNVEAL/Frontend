package com.swyp.runveal.running.contract

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.ui.mvi.UiState

enum class RecordingPhase { IDLE, RECORDING, PAUSED }

/** `elapsedLabel`/`distanceLabel`/`paceLabel`/`newRoadLabel`은 이미 포맷된 문자열로 들고 있는다(화면은
 * 그대로 쓰기만 한다). [distanceLabel]은 단위(km) 없이 숫자만 — 디자인이 숫자/단위를 다른 스타일로
 * 그리기 때문. [selectedCoursePath]는 "이 코스로 러닝 시작"으로 넘어온 코스 경로 — 지금은 보관만 하고
 * 지도 가이드/오버레이로 그리는 건 보류. */
data class RecordingState(
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val elapsedLabel: String = "00:00",
    val distanceLabel: String = "0.00",
    val paceLabel: String = "0'00″",
    /** 이번 러닝에서 처음 가보는 길만 합친 거리(km, 단위 없이 숫자만) — [distanceLabel](총 거리)과
     * 달리 이미 가본 구간은 빠진다. 지도가 금색으로 칠하는 선과 같은 분류 결과를 쓴다
     * ([com.swyp.runveal.result.LiveNewRoadHolder]). */
    val newRoadLabel: String = "0.00",
    val selectedCoursePath: List<LatLon>? = null,
    /** 선택한 코스가 순환이면 출발·도착이 같은 자리라 도착 핀을 생략한다(코스 상세와 같은 규칙). */
    val selectedCourseIsLoop: Boolean = false,
) : UiState
