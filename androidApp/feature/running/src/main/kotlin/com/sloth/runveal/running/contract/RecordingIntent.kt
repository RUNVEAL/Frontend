package com.sloth.runveal.running.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface RecordingIntent : UiIntent {
    data object StartClicked : RecordingIntent
    data object PauseClicked : RecordingIntent
    data object ResumeClicked : RecordingIntent
    /** "종료하기"를 눌렀다 — 바로 끝내지 않고 확인 다이얼로그를 띄운다. */
    data object StopRequested : RecordingIntent

    /** 확인 다이얼로그에서 취소했다(바깥 탭·뒤로가기 포함) — 일시정지 상태로 돌아간다. */
    data object StopDismissed : RecordingIntent

    /** 확인 다이얼로그에서 "끝내기"를 눌렀다 — 여기서 실제로 기록을 멈춘다. */
    data object StopClicked : RecordingIntent
}
