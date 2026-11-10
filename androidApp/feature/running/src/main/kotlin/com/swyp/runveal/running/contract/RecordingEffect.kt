package com.swyp.runveal.running.contract

import com.swyp.runveal.ui.mvi.UiEffect

/** `TrackingService`는 `Context`가 있어야 호출할 수 있어 ViewModel이 직접 부르지 않는다 — 화면이
 * [com.swyp.runveal.ui.mvi.CollectEffect]로 받아 실행한다. */
sealed interface RecordingEffect : UiEffect {
    data object StartTrackingService : RecordingEffect
    data object PauseTrackingService : RecordingEffect
    data object ResumeTrackingService : RecordingEffect
    data object StopTrackingService : RecordingEffect
}
