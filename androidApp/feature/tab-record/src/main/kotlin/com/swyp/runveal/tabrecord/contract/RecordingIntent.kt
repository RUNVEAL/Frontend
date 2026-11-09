package com.swyp.runveal.tabrecord.contract

import com.swyp.runveal.ui.mvi.UiIntent

sealed interface RecordingIntent : UiIntent {
    data object StartClicked : RecordingIntent
    data object PauseClicked : RecordingIntent
    data object ResumeClicked : RecordingIntent
    data object StopClicked : RecordingIntent
}
