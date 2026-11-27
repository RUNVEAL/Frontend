package com.sloth.runveal.running.contract

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.ui.mvi.UiState

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
     * ([com.sloth.runveal.result.LiveNewRoadHolder]). */
    val newRoadLabel: String = "0.00",
    val selectedCoursePath: List<LatLon>? = null,
    /** 선택한 코스가 순환이면 출발·도착이 같은 자리라 도착 핀을 생략한다(코스 상세와 같은 규칙). */
    val selectedCourseIsLoop: Boolean = false,
    /** 저장된 코스로 시작한 경우 그 코스의 id — 기록이 저장될 때 "달림"으로 적는 데 쓴다.
     * 자유 러닝이나 저장 전 추천 코스는 null이다. */
    val selectedCourseId: String? = null,
    /** 종료 확인 다이얼로그 — 떠 있지 않으면 null. 종료는 되돌릴 수 없어서 한 번 더 묻는다
     * (일시정지 중에만 뜬다 — 종료 버튼 자체가 그때만 보인다). */
    val finishDialog: FinishDialog? = null,
) : UiState

/**
 * 종료 확인 다이얼로그에 넘길 것 — [willSaveRecord]가 false면 100m/1분을 못 채워 **저장되지 않는**
 * 러닝이라, 문구와 버튼이 그 사실을 알리는 쪽으로 바뀐다([com.sloth.runveal.recording.RecordingSavePolicy]).
 * 저장 여부는 다이얼로그를 띄울 때 재서 담아 둔다 — 일시정지 중이라 그 뒤로 거리·시간이 늘지 않는다.
 */
data class FinishDialog(val willSaveRecord: Boolean)
