package com.sloth.runveal.tabrecord

import android.util.Log
import com.sloth.runveal.auth.AndroidAppContextHolder
import com.sloth.runveal.core.map.DongAssetRepository
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.toSnapshotRequest
import com.sloth.runveal.result.DongRateChanges
import com.sloth.runveal.tabrecord.contract.RecordDetailDongStat
import com.sloth.runveal.tabrecord.contract.RecordDetailIntent
import com.sloth.runveal.tabrecord.contract.RecordDetailState
import com.sloth.runveal.ui.mvi.MviViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "RecordDetail"

/** 칼로리는 아직 어디에도 저장하지 않는다 — 값이 생기기 전까지 자리만 지킨다. */
private const val CALORIES_PLACEHOLDER = "-"

/** 효과가 필요 없는 화면이라 Effect 타입 인자는 [Nothing]. */
class RecordDetailViewModel(
    private val store: TrackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore()),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : MviViewModel<RecordDetailIntent, RecordDetailState, Nothing>(initialState = RecordDetailState.Loading) {

    override suspend fun handleIntent(intent: RecordDetailIntent) {
        when (intent) {
            is RecordDetailIntent.Load -> load(intent.trackId)
        }
    }

    private suspend fun load(trackId: String) {
        val track = withContext(Dispatchers.IO) { store.trackSummary(trackId) }
        if (track == null) {
            setState { RecordDetailState.NotFound }
            return
        }

        // 목록 썸네일과 같은 재료를 쓰되 크기만 결과 카드(RESULT_CARD)로 — 같은 경로라도 자리마다
        // 따로 캐시된다(CourseSnapshotRenderer 주석).
        val route = withContext(Dispatchers.IO) { store.routesForTracks(listOf(trackId))[trackId].orEmpty() }
        val newRoadPaths = withContext(Dispatchers.IO) { store.newRoadPathsForTracks(listOf(trackId))[trackId].orEmpty() }
        val newRoadMeters = withContext(Dispatchers.IO) { store.newRoadMetersForTrack(trackId) }
        val dongStats = withContext(Dispatchers.IO) { loadDongStats(trackId) }

        val startedAt = Instant.ofEpochMilli(track.startedAtEpochMs).atZone(zoneId)
        setState {
            RecordDetailState.Ready(
                // 역지오코딩으로 장소명을 채우기 전까지는 "저녁 러닝"처럼 시간대로 부른다(기록 탭과 같은 규칙).
                title = track.placeName ?: "${timeOfDayLabel(startedAt)} 러닝",
                dateTimeLabel = startedAt.format(DATE_TIME_FORMATTER),
                distanceValue = "%.2f".format(track.distanceMeters / 1000.0),
                newRoadValue = "+%.1f".format(newRoadMeters / 1000.0),
                elapsedLabel = formatElapsed(track.elapsedMs),
                paceLabel = formatPace(track.elapsedMs, track.distanceMeters),
                caloriesLabel = CALORIES_PLACEHOLDER,
                dongStats = dongStats,
                mapSnapshotRequest = route.toSnapshotRequest(
                    ownerId = trackId,
                    variant = SnapshotVariant.RESULT_CARD,
                    newRoadPaths = newRoadPaths,
                ),
            )
        }
    }

    /**
     * 그 러닝이 지난 동들의 탐험률 전/후. 분자는 기록 재생([TrackLocalStore.dongWalkedAroundTrack]),
     * 분모는 구간 인덱스 — 러닝 결과 화면과 **같은 공식**을 쓰려고 [DongRateChanges]를 거친다.
     */
    private fun loadDongStats(trackId: String): List<RecordDetailDongStat> {
        val walked = store.dongWalkedAroundTrack(trackId)
        if (walked.isEmpty()) return emptyList()
        return DongRateChanges.build(
            touchedAdmCodes = walked.map { it.admCd2 }.toSet(),
            beforeWalked = walked.associate { it.admCd2 to it.beforeMeters },
            afterWalked = walked.associate { it.admCd2 to it.afterMeters },
            totalLengths = store.totalLengthByDong(),
            namesByCode = loadDongNames(),
        ).map { RecordDetailDongStat(name = it.name, beforePercent = it.beforePercent, afterPercent = it.afterPercent) }
    }

    /** 이름을 못 읽으면 탐험률 줄만 비운다 — 나머지 기록은 그대로 보여준다(HealthConnectImporter와 같은 처리). */
    private fun loadDongNames(): Map<String, String> = runCatching {
        DongAssetRepository(AndroidAppContextHolder.context).loadRegionHierarchy().sido
            .flatMap { sido -> sido.sigungu.flatMap { it.dongs } }
            .associate { it.code to it.name }
    }.onFailure { Log.w(TAG, "동 이름 읽기 실패 — 탐험률 줄을 비운다", it) }
        .getOrDefault(emptyMap())

    /** 기록 탭과 같은 구간 기준. */
    private fun timeOfDayLabel(dateTime: ZonedDateTime): String = when (dateTime.hour) {
        in 5..10 -> "아침"
        in 11..13 -> "점심"
        in 14..17 -> "오후"
        in 18..21 -> "저녁"
        else -> "밤"
    }

    /** 한 시간을 넘긴 기록도 들어오므로 1시간 이상이면 `h:mm:ss`로 늘린다(기록 탭과 같은 규칙). */
    private fun formatElapsed(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }

    /** "6'05″" 형태 — km당 분'초″. 기록 탭·결과 화면과 같은 공식. */
    private fun formatPace(elapsedMs: Long, distanceMeters: Double): String {
        if (distanceMeters <= 0.0) return "0'00″"
        val paceSecondsPerKm = (elapsedMs / 1000.0) / (distanceMeters / 1000.0)
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${seconds.toString().padStart(2, '0')}″"
    }

    private companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    }
}
