package com.swyp.runveal.running

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.recording.RecordingState as TrackRecordingState
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.running.contract.RecordingEffect
import com.swyp.runveal.running.contract.RecordingIntent
import com.swyp.runveal.running.contract.RecordingPhase
import com.swyp.runveal.running.contract.RecordingState
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TICKER_INTERVAL_MS = 1000L

class RecordingViewModel : MviViewModel<RecordingIntent, RecordingState, RecordingEffect>(
    initialState = RecordingState(),
) {
    init {
        viewModelScope.launch {
            TrackRecorder.state.collect { recording ->
                setState {
                    when (recording) {
                        is TrackRecordingState.Recording -> copy(phase = RecordingPhase.RECORDING)
                        is TrackRecordingState.Paused -> copy(phase = RecordingPhase.PAUSED)
                        else -> RecordingState()
                    }
                }
            }
        }
        // `TrackRecorder.state`는 위치가 들어올 때만 갱신되므로, 경과 시간/페이스 표시는 따로 1초마다
        // 갈아준다. 일시정지 중에도 돌긴 하지만 TrackRecorder.elapsedMs()가 멈춘 시점에 고정값을 주므로
        // 화면엔 숫자가 그대로 멈춰 보인다.
        viewModelScope.launch {
            while (true) {
                val recording = TrackRecorder.state.value
                if (recording is TrackRecordingState.Recording || recording is TrackRecordingState.Paused) {
                    val elapsedMs = TrackRecorder.elapsedMs(System.currentTimeMillis())
                    val distanceMeters = TrackRecorder.totalDistanceMeters()
                    setState {
                        copy(
                            elapsedLabel = formatElapsed(elapsedMs),
                            distanceLabel = "%.2f".format(distanceMeters / 1000.0),
                            paceLabel = formatPace(elapsedMs, distanceMeters),
                        )
                    }
                }
                delay(TICKER_INTERVAL_MS)
            }
        }
    }

    override suspend fun handleIntent(intent: RecordingIntent) {
        when (intent) {
            RecordingIntent.StartClicked -> sendEffect(RecordingEffect.StartTrackingService)
            RecordingIntent.PauseClicked -> sendEffect(RecordingEffect.PauseTrackingService)
            RecordingIntent.ResumeClicked -> sendEffect(RecordingEffect.ResumeTrackingService)
            RecordingIntent.StopClicked -> sendEffect(RecordingEffect.StopTrackingService)
        }
    }

    private fun formatElapsed(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /** "6'02″" 형태 — km당 분'초″. */
    private fun formatPace(elapsedMs: Long, distanceMeters: Double): String {
        if (distanceMeters <= 0.0) return "0'00″"
        val paceSecondsPerKm = (elapsedMs / 1000.0) / (distanceMeters / 1000.0)
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${seconds.toString().padStart(2, '0')}″"
    }
}
