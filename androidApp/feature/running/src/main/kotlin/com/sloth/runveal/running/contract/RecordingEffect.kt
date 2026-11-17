package com.sloth.runveal.running.contract

import com.sloth.runveal.ui.mvi.UiEffect

/** `TrackingService`는 `Context`가 있어야 호출할 수 있어 ViewModel이 직접 부르지 않는다 — 화면이
 * [com.sloth.runveal.ui.mvi.CollectEffect]로 받아 실행한다. */
sealed interface RecordingEffect : UiEffect {
    data object StartTrackingService : RecordingEffect
    data object PauseTrackingService : RecordingEffect
    data object ResumeTrackingService : RecordingEffect
    /** [saveRecord]가 false면 너무 짧아 버리는 러닝 — 서비스도 저장을 건너뛰고, 화면도 결과 화면
     * 대신 홈으로 돌아간다([com.sloth.runveal.recording.RecordingSavePolicy]). */
    data class StopTrackingService(val saveRecord: Boolean) : RecordingEffect
}
