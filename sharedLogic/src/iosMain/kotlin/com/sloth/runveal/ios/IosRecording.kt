package com.sloth.runveal.ios

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.recording.RecordingState
import com.sloth.runveal.recording.TrackPoint
import com.sloth.runveal.recording.TrackRecorder

/** [RecordingSnapshot.phase] — Kotlin sealed interface를 Swift가 다루기 쉬운 enum으로 평평하게 만든 것. */
enum class RecordingPhase {
    IDLE,
    RECORDING,
    PAUSED,
    FINISHED,
}

/**
 * [RecordingState]를 Swift에서 쓰기 쉬운 한 덩어리로 평평하게 만든 값.
 *
 * Kotlin sealed interface + 중첩 data class는 Objective-C로 나갈 때 중첩이 풀려서 Swift 쪽 이름이
 * 직관적이지 않고, `is`/`as?` 분기도 Swift에서 지저분해진다. iOS 화면은 이 스냅샷만 본다.
 */
data class RecordingSnapshot(
    val phase: RecordingPhase,
    /** `RECORDING`/`PAUSED`/`FINISHED`에서만 값이 있다. */
    val id: String?,
    val startedAtEpochMs: Long,
    /** `FINISHED`에서만 값이 있다(그 외 0). */
    val finishedAtEpochMs: Long,
    /** `FINISHED`에서만 채워진다. */
    val filteredPoints: List<TrackPoint>,
) {
    val isRecordingOrPaused: Boolean
        get() = phase == RecordingPhase.RECORDING || phase == RecordingPhase.PAUSED
}

internal fun RecordingState.toSnapshot(): RecordingSnapshot = when (this) {
    is RecordingState.Idle -> RecordingSnapshot(
        phase = RecordingPhase.IDLE,
        id = null,
        startedAtEpochMs = 0,
        finishedAtEpochMs = 0,
        filteredPoints = emptyList(),
    )

    is RecordingState.Recording -> RecordingSnapshot(
        phase = RecordingPhase.RECORDING,
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = 0,
        filteredPoints = emptyList(),
    )

    is RecordingState.Paused -> RecordingSnapshot(
        phase = RecordingPhase.PAUSED,
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = 0,
        filteredPoints = emptyList(),
    )

    is RecordingState.Finished -> RecordingSnapshot(
        phase = RecordingPhase.FINISHED,
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        filteredPoints = filteredPoints,
    )
}

/**
 * [TrackRecorder]를 iOS에서 쓰기 위한 얇은 파사드 — 상태를 [RecordingSnapshot]으로 돌려주고,
 * 나머지는 그대로 위임한다. Swift가 `RecordingState` sealed 계층을 직접 다루지 않게 하기 위함이다.
 */
object RunvealRecorder {

    fun snapshot(): RecordingSnapshot = TrackRecorder.state.value.toSnapshot()

    fun start(id: String, startedAtEpochMs: Long) = TrackRecorder.start(id, startedAtEpochMs)

    fun pause(nowEpochMs: Long) = TrackRecorder.pause(nowEpochMs)

    fun resume(nowEpochMs: Long) = TrackRecorder.resume(nowEpochMs)

    fun reset() = TrackRecorder.reset()

    /** 필터에 걸려 버려졌으면 그 이유 이름(`DropReason`), 통과했거나 녹화 중이 아니면 null. */
    fun addRawPoint(
        lat: Double,
        lon: Double,
        accuracyMeters: Float,
        speedMps: Float,
        timeEpochMs: Long,
    ): String? = TrackRecorder.addRawPoint(
        TrackPoint(
            lat = lat,
            lon = lon,
            accuracyMeters = accuracyMeters,
            speedMps = speedMps,
            timeEpochMs = timeEpochMs,
        ),
    )?.name

    fun elapsedMs(nowEpochMs: Long): Long = TrackRecorder.elapsedMs(nowEpochMs)

    fun totalDistanceMeters(): Double = TrackRecorder.totalDistanceMeters()

    /** 지금까지 필터를 통과한 점들의 좌표만 — 지도에 "지금 달리는 선"을 그릴 때 쓴다. */
    fun currentFilteredPath(): List<LatLon> =
        TrackRecorder.currentFilteredPoints().map { LatLon(lat = it.lat, lon = it.lon) }

    /** `RECORDING`/`PAUSED` → `FINISHED`로 바꾸고 그 스냅샷을 돌려준다. 그 외 상태면 null. */
    fun finish(finishedAtEpochMs: Long): RecordingSnapshot? =
        TrackRecorder.finish(finishedAtEpochMs)?.toSnapshot()
}
