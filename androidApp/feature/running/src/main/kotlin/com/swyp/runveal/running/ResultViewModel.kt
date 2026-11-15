package com.swyp.runveal.running

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.map.snapshot.SnapshotVariant
import com.swyp.runveal.map.snapshot.toSnapshotRequest
import com.swyp.runveal.result.TrackResult
import com.swyp.runveal.result.TrackResultHolder
import com.swyp.runveal.running.contract.ResultDongStat
import com.swyp.runveal.running.contract.ResultState
import com.swyp.runveal.ui.mvi.MviViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

/** 사용자가 보낼 의도가 없는 화면(버튼은 전부 네비게이션/TODO) — Intent 타입 인자로 [Nothing]을 쓴다. */
class ResultViewModel : MviViewModel<Nothing, ResultState, Nothing>(initialState = ResultState.Loading) {
    init {
        viewModelScope.launch {
            TrackResultHolder.state.collect { result ->
                setState { result?.let(::toReadyState) ?: ResultState.Loading }
            }
        }
    }

    override suspend fun handleIntent(intent: Nothing) {}

    private fun toReadyState(result: TrackResult): ResultState.Ready {
        val distanceKm = result.distanceMeters / 1000.0
        return ResultState.Ready(
            dateLabel = formatDate(result.startedAtEpochMs),
            newRoadLabel = "%.1f".format(result.newRoadMeters / 1000.0),
            dongStats = result.dongStats.map { ResultDongStat(it.name, it.beforePercent, it.afterPercent) },
            distanceLabel = "%.2f".format(distanceKm),
            elapsedLabel = formatElapsed(result.elapsedMs),
            paceLabel = formatPace(result.elapsedMs, result.distanceMeters),
            mapSnapshotRequest = result.routePoints.toSnapshotRequest(
                ownerId = result.trackId,
                variant = SnapshotVariant.RESULT_CARD,
                newRoadPaths = result.newRoadPaths,
            ),
        )
    }

    private fun formatDate(epochMs: Long): String {
        val dateTime = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        val dayOfWeek = when (dateTime.dayOfWeek) {
            DayOfWeek.MONDAY -> "월요일"
            DayOfWeek.TUESDAY -> "화요일"
            DayOfWeek.WEDNESDAY -> "수요일"
            DayOfWeek.THURSDAY -> "목요일"
            DayOfWeek.FRIDAY -> "금요일"
            DayOfWeek.SATURDAY -> "토요일"
            DayOfWeek.SUNDAY -> "일요일"
        }
        val timeOfDay = when (dateTime.hour) {
            in 5..10 -> "아침"
            in 11..13 -> "점심"
            in 14..17 -> "오후"
            in 18..21 -> "저녁"
            else -> "밤"
        }
        return "${dateTime.monthValue}월 ${dateTime.dayOfMonth}일 $dayOfWeek $timeOfDay 러닝"
    }

    private fun formatElapsed(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /** "6'05″" 형태 — km당 분'초″. `tab-record`의 `RecordingViewModel.formatPace`와 같은 공식. */
    private fun formatPace(elapsedMs: Long, distanceMeters: Double): String {
        if (distanceMeters <= 0.0) return "0'00″"
        val paceSecondsPerKm = (elapsedMs / 1000.0) / (distanceMeters / 1000.0)
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${seconds.toString().padStart(2, '0')}″"
    }
}
