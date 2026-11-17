package com.sloth.runveal.recording

import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val id: String, val startedAtEpochMs: Long) : RecordingState
    data class Paused(val id: String, val startedAtEpochMs: Long) : RecordingState
    data class Finished(
        val id: String,
        val startedAtEpochMs: Long,
        val finishedAtEpochMs: Long,
        val filteredPoints: List<TrackPoint>,
        val rawPoints: List<TrackPoint>,
        val dropCounts: Map<DropReason, Int>,
    ) : RecordingState
}

/**
 * 기록 상태 싱글톤(StateFlow) — `docs/running/recording-lifecycle.md` §2/§3/§4.1. 위치 수신 서비스와
 * UI(진행 표시)가 같이 구독한다. raw/filtered 점을 메모리에 전부 들고 있다가 종료 시 GPX 본문을 한
 * 번에 만든다 — "기록 중 append"(문서 원안) 대신 "종료 시 한 번에 쓰기"로 단순화했다. 어차피 `track`
 * row 자체가 녹화가 끝난 뒤에만 DB에 올라가므로(local-db §5 결정), GPX도 끝나야 쓰이는 건 새로운
 * 손실 지점이 아니다 — 녹화 중 프로세스가 죽으면 지금 설계에서도 그 기록은 어차피 복구 대상이 아니다.
 *
 * 일시정지는 문서 원안엔 없던 UI 요구사항으로 추가됐다 — `Paused` 동안은 [addRawPoint]가 무시되고
 * (서비스가 위치 수신 자체를 멈추는 게 정상 흐름), [elapsedMs]도 멈춘 시점에 고정된다.
 */
object TrackRecorder {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val filteredPoints = mutableListOf<TrackPoint>()
    private val rawPoints = mutableListOf<TrackPoint>()
    private val dropCounts = mutableMapOf<DropReason, Int>()

    /** 지금까지 일시정지로 빠진 누적 시간. [elapsedMs]가 이만큼을 경과 시간에서 뺀다. */
    private var pausedDurationMs: Long = 0
    private var pausedAtEpochMs: Long? = null

    /** 이미 `Recording`이면 무시한다 — 진행 중인 기록이 빈 기록으로 리셋되면 안 된다(§3 "중복 시작 금지"). */
    fun start(id: String, startedAtEpochMs: Long) {
        if (_state.value is RecordingState.Recording) return
        filteredPoints.clear()
        rawPoints.clear()
        dropCounts.clear()
        pausedDurationMs = 0
        pausedAtEpochMs = null
        _state.value = RecordingState.Recording(id, startedAtEpochMs)
    }

    /** `Recording`이 아니면 무시. */
    fun pause(nowEpochMs: Long) {
        val current = _state.value as? RecordingState.Recording ?: return
        pausedAtEpochMs = nowEpochMs
        _state.value = RecordingState.Paused(current.id, current.startedAtEpochMs)
    }

    /** `Paused`가 아니면 무시. */
    fun resume(nowEpochMs: Long) {
        val current = _state.value as? RecordingState.Paused ?: return
        pausedAtEpochMs?.let { pausedDurationMs += nowEpochMs - it }
        pausedAtEpochMs = null
        _state.value = RecordingState.Recording(current.id, current.startedAtEpochMs)
    }

    /** raw 점 수신 — 필터를 돌려 통과하면 filtered 목록에도 추가한다. 녹화 중이 아니면 무시(null). */
    fun addRawPoint(point: TrackPoint): DropReason? {
        if (_state.value !is RecordingState.Recording) return null
        rawPoints.add(point)
        val reason = TrackFilter.evaluate(point, filteredPoints.lastOrNull())
        if (reason == null) {
            filteredPoints.add(point)
        } else {
            dropCounts[reason] = (dropCounts[reason] ?: 0) + 1
        }
        return reason
    }

    fun currentFilteredPoints(): List<TrackPoint> = filteredPoints.toList()

    fun totalDistanceMeters(): Double =
        GeoMath.totalDistanceMeters(filteredPoints.map { LatLon(lat = it.lat, lon = it.lon) })

    /** `Recording`/`Paused` 둘 다에서 호출 가능 — 일시정지 중 종료도 지원해야 한다(디자인 "종료하기"). */
    fun elapsedMs(nowEpochMs: Long): Long {
        val startedAt = when (val current = _state.value) {
            is RecordingState.Recording -> current.startedAtEpochMs
            is RecordingState.Paused -> current.startedAtEpochMs
            else -> return 0
        }
        val pausedSoFar = pausedDurationMs + (pausedAtEpochMs?.let { nowEpochMs - it } ?: 0)
        return (nowEpochMs - startedAt - pausedSoFar).coerceAtLeast(0)
    }

    /** `Recording`/`Paused` → `Finished`로 바꾸고 그 스냅샷을 돌려준다. 그 외 상태면 null. */
    fun finish(finishedAtEpochMs: Long): RecordingState.Finished? {
        val (id, startedAtEpochMs) = when (val current = _state.value) {
            is RecordingState.Recording -> current.id to current.startedAtEpochMs
            is RecordingState.Paused -> current.id to current.startedAtEpochMs
            else -> return null
        }
        val finished = RecordingState.Finished(
            id = id,
            startedAtEpochMs = startedAtEpochMs,
            finishedAtEpochMs = finishedAtEpochMs,
            filteredPoints = filteredPoints.toList(),
            rawPoints = rawPoints.toList(),
            dropCounts = dropCounts.toMap(),
        )
        _state.value = finished
        return finished
    }

    fun reset() {
        _state.value = RecordingState.Idle
    }
}
