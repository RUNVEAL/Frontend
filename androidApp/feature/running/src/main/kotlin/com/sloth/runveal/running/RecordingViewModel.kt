package com.sloth.runveal.running

import androidx.lifecycle.viewModelScope
import com.sloth.runveal.analytics.RunvealAnalyticsLogger
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.recording.RecordingSavePolicy
import com.sloth.runveal.recording.RecordingState as TrackRecordingState
import com.sloth.runveal.recording.TrackRecorder
import com.sloth.runveal.result.LiveNewRoadHolder
import com.sloth.runveal.result.SelectedCourseHolder
import com.sloth.runveal.running.contract.FinishDialog
import com.sloth.runveal.running.contract.RecordingEffect
import com.sloth.runveal.running.contract.RecordingIntent
import com.sloth.runveal.running.contract.RecordingPhase
import com.sloth.runveal.running.contract.RecordingState
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TICKER_INTERVAL_MS = 1000L

/** "이 코스로 러닝 시작"으로 넘어온 코스를 한 번만 꺼내 초기 상태에 담는다 — 홀더는 읽는 순간
 * 비워지므로([SelectedCourseHolder.consume]) 생성자에서 딱 한 번만 읽어야 한다. */
private fun initialRecordingState(): RecordingState {
    val selected = SelectedCourseHolder.consume()
    return RecordingState(
        selectedCoursePath = selected?.path,
        selectedCourseIsLoop = selected?.isLoop == true,
        selectedCourseId = selected?.courseId,
    )
}

class RecordingViewModel : MviViewModel<RecordingIntent, RecordingState, RecordingEffect>(
    initialState = initialRecordingState(),
) {
    init {
        // 러닝 기록 화면 진입 — ViewModel이 만들어지는 이 시점이 곧 화면이 열린 시점이고, 들어온
        // 길(자유 러닝/코스)도 여기서 알 수 있다(위 initialRecordingState가 홀더에서 꺼내 둔다).
        //
        // 네비게이션 호출부가 아니라 여기서 쏘는 이유: 기록 화면으로 오는 길이 넷이고
        // (홈 줌인, 추천 코스 상세, 내 코스 상세, 내 코스 목록) 그중 일부는 다른 모듈에 있어서,
        // 호출부마다 심으면 새 길이 생길 때 조용히 빠진다. 화면은 한 곳이니 여기가 빠뜨릴 수 없다.
        RunvealAnalyticsLogger.logRunStart(hasCourse = currentState.selectedCoursePath != null)

        viewModelScope.launch {
            TrackRecorder.state.collect { recording ->
                setState {
                    when (recording) {
                        is TrackRecordingState.Recording -> copy(phase = RecordingPhase.RECORDING)
                        is TrackRecordingState.Paused -> copy(phase = RecordingPhase.PAUSED)
                        // 코스 데이터(selectedCoursePath/selectedCourseIsLoop)는 그대로 들고 있는다 —
                        // 전체 리셋을 쓰면 위에서 미리 읽어 둔 값이 지워진다.
                        else -> RecordingState(
                            selectedCoursePath = selectedCoursePath,
                            selectedCourseIsLoop = selectedCourseIsLoop,
                        )
                    }
                }
            }
        }
        // "새로 탐험한 길"은 지도(HomeScreen)가 1초마다 돌리는 LiveRoadClassifier 결과를 그대로 받아
        // 쓴다 — 여기서 또 분류하면 전국 구간 인덱스를 매초 두 번 훑게 되고, 지도 금색 선과 배지
        // 숫자가 서로 다른 계산을 근거로 삼게 된다.
        viewModelScope.launch {
            LiveNewRoadHolder.state.collect { newRoadMeters ->
                setState { copy(newRoadLabel = "%.2f".format(newRoadMeters / 1000.0)) }
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
            RecordingIntent.StopRequested -> setState {
                copy(finishDialog = FinishDialog(willSaveRecord = isRecordSavable()))
            }
            RecordingIntent.StopDismissed -> setState { copy(finishDialog = null) }
            // 저장 여부는 여기서 한 번만 정해 서비스까지 그대로 넘긴다 — 서비스가 따로 다시 판단하면
            // 그 사이 1분/100m 경계를 넘겨서 "홈으로 돌려보냈는데 기록은 저장됨" 같은 어긋남이 생긴다.
            RecordingIntent.StopClicked -> {
                val saveRecord = isRecordSavable()
                setState { copy(finishDialog = null) }
                // 기록이 **저장될 때만** 그 코스를 달린 것으로 적는다 — 100m·1분을 못 채워 버려지는
                // 러닝까지 세면 "달렸다"는 표시가 실제 기록과 어긋난다.
                if (saveRecord) markSelectedCourseRun()
                sendEffect(RecordingEffect.StopTrackingService(saveRecord = saveRecord))
            }
        }
    }

    /** 저장된 코스로 달렸으면 그 코스에 마지막 달린 시각을 적는다 — "다음에 달려볼 코스" 카드가
     * 아직 안 달린 코스만 고르는 데 쓴다. 자유 러닝이면 적을 코스가 없어 그냥 지나간다. */
    private fun markSelectedCourseRun() {
        val courseId = currentState.selectedCourseId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            RunvealDatabaseProvider.database.courseQueries
                .markCourseRun(System.currentTimeMillis(), courseId)
        }
    }

    /** 지금 끝내면 기록이 저장되는지(100m·1분, `RecordingSavePolicy`). 다이얼로그 문구와 실제 저장
     * 판정이 같은 값을 쓰도록 한 곳에 둔다. */
    private fun isRecordSavable(): Boolean = RecordingSavePolicy.isSavable(
        distanceMeters = TrackRecorder.totalDistanceMeters(),
        elapsedMs = TrackRecorder.elapsedMs(System.currentTimeMillis()),
    )

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
