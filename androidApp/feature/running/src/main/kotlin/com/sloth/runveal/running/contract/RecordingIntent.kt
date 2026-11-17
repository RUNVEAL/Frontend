package com.sloth.runveal.running.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface RecordingIntent : UiIntent {
    data object StartClicked : RecordingIntent
    data object PauseClicked : RecordingIntent
    data object ResumeClicked : RecordingIntent
    data object StopClicked : RecordingIntent
}
