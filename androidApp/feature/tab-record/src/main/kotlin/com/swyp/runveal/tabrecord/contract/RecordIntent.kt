package com.swyp.runveal.tabrecord.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface RecordIntent : UiIntent {
    /** 기록을 길게 눌렀다 — 바로 지우지 않고 확인 다이얼로그를 띄운다. */
    data class DeleteRequested(val trackId: String) : RecordIntent

    data object DeleteDismissed : RecordIntent
    data object DeleteConfirmed : RecordIntent
}
