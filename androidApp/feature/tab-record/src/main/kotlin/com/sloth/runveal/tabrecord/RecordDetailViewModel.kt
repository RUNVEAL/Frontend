package com.sloth.runveal.tabrecord

import android.util.Log
import com.sloth.runveal.auth.AndroidAppContextHolder
import com.sloth.runveal.core.map.DongAssetRepository
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.toSnapshotRequest
import com.sloth.runveal.recording.RunSplits
import com.sloth.runveal.result.DongRateChanges
import com.sloth.runveal.tabrecord.contract.RecordDetailDongStat
import com.sloth.runveal.tabrecord.contract.RecordDetailEffect
import com.sloth.runveal.tabrecord.contract.RecordDetailIntent
import com.sloth.runveal.tabrecord.contract.RecordDetailSplit
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

/** 가장 느린 구간이라도 막대가 보이게 두는 최소 길이 비율. */
private const val MIN_SPLIT_BAR_RATIO = 0.08f

/** 이 차이 이내면 같은 페이스로 본다(km당 초) — 라벨도 막대도 구분이 안 가는 수준이다. */
private const val PACE_EQUAL_TOLERANCE_SECONDS = 1.0

class RecordDetailViewModel(
    private val store: TrackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore()),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : MviViewModel<RecordDetailIntent, RecordDetailState, RecordDetailEffect>(initialState = RecordDetailState.Loading) {

    /** 지금 보고 있는 기록 — 삭제할 때 다시 쓴다(상태는 화면에 찍을 라벨만 들고 있다). */
    private var trackId: String? = null

    override suspend fun handleIntent(intent: RecordDetailIntent) {
        when (intent) {
            is RecordDetailIntent.Load -> load(intent.trackId)
            RecordDetailIntent.RenameClicked ->
                setState { (this as? RecordDetailState.Ready)?.copy(isRenameSheetVisible = true) ?: this }
            RecordDetailIntent.RenameDismissed ->
                setState { (this as? RecordDetailState.Ready)?.copy(isRenameSheetVisible = false) ?: this }
            is RecordDetailIntent.RenameConfirmed -> rename(intent.name)
            RecordDetailIntent.DeleteClicked ->
                setState { (this as? RecordDetailState.Ready)?.copy(isDeleteDialogVisible = true) ?: this }
            RecordDetailIntent.DeleteDismissed ->
                setState { (this as? RecordDetailState.Ready)?.copy(isDeleteDialogVisible = false) ?: this }
            RecordDetailIntent.DeleteConfirmed -> deleteTrack()
        }
    }

    /** 이름을 바꾸고 화면을 다시 읽는다 — 상단 제목이 바로 갈린다. 공백만 들어오면 아무것도 하지
     * 않는다(시트의 "저장" 버튼도 같은 조건으로 꺼진다). */
    private suspend fun rename(name: String) {
        val id = trackId ?: return
        val newName = name.trim()
        if (newName.isEmpty()) return
        setState { (this as? RecordDetailState.Ready)?.copy(isRenameSheetVisible = false) ?: this }
        withContext(Dispatchers.IO) { store.renameTrack(id, newName, System.currentTimeMillis()) }
        load(id)
    }

    /**
     * 기록 한 건을 지운다. [TrackLocalStore.deleteTrack]이 GPX 파일까지 같이 지우고
     * `explored_segment` 커버리지를 남은 기록만으로 다시 만든다 — 그래서 **지운 러닝이 밝혀 둔 길과
     * 탐험률도 같이 줄어든다**. 그 캐시를 다시 만드는 비용이 있어 IO로 넘긴다(기록 탭에 있던 처리를
     * 그대로 옮겨 왔다).
     */
    private suspend fun deleteTrack() {
        val id = trackId ?: return
        setState { (this as? RecordDetailState.Ready)?.copy(isDeleteDialogVisible = false) ?: this }
        withContext(Dispatchers.IO) { store.deleteTrack(id, System.currentTimeMillis()) }
        sendEffect(RecordDetailEffect.NavigateBack)
    }

    private suspend fun load(trackId: String) {
        this.trackId = trackId
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
        val splits = withContext(Dispatchers.IO) { loadSplits(trackId) }

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
                splits = splits,
                mapSnapshotRequest = route.toSnapshotRequest(
                    ownerId = trackId,
                    variant = SnapshotVariant.RESULT_CARD,
                    newRoadPaths = newRoadPaths,
                ),
            )
        }
    }

    /**
     * 1km 구간별 페이스. 저장해 둔 GPX를 읽어([GpxPoints]) 공용 계산([RunSplits])에 넘긴다 — 거리는
     * `track.distance_meters`가 아니라 점 사이를 다시 재서 쌓으므로 구간 경계가 실제 경로를 따른다.
     *
     * 막대 길이는 **가장 빠른 구간을 1로 둔 비율**이다(느릴수록 짧다). 0에 가까워 안 보이는 일이
     * 없게 최소 길이를 둔다 — 그래서 전 구간 페이스가 같으면 전부 꽉 찬다.
     *
     * 강조(가장 빠른 구간)는 **구간들 사이에 차이가 있을 때만** 켠다 — 다 같은 페이스면 "가장 빠른
     * 구간"이랄 게 없다. 구간이 하나뿐이면 그 줄이 곧 가장 빠른 구간이라 켠다. 페이스는 실수라
     * 딱 떨어지게 같을 일이 드물어, 눈에도 라벨에도 차이가 없는 [PACE_EQUAL_TOLERANCE_SECONDS]
     * 이내는 같은 것으로 본다.
     */
    private fun loadSplits(trackId: String): List<RecordDetailSplit> {
        val gpx = store.filteredGpx(trackId) ?: return emptyList()
        val splits = RunSplits.of(GpxPoints.parse(gpx))
        if (splits.isEmpty()) return emptyList()
        val paces = splits.map { it.paceSecondsPerKm }
        val fastestPace = paces.min().takeIf { it > 0.0 } ?: return emptyList()
        val hasFasterSplit = splits.size == 1 || (paces.max() - fastestPace) > PACE_EQUAL_TOLERANCE_SECONDS
        return splits.map { split ->
            RecordDetailSplit(
                label = "${split.index}km",
                paceLabel = formatPaceSeconds(split.paceSecondsPerKm),
                barRatio = (fastestPace / split.paceSecondsPerKm).toFloat()
                    .coerceIn(MIN_SPLIT_BAR_RATIO, 1f),
                isFastest = hasFasterSplit &&
                    (split.paceSecondsPerKm - fastestPace) <= PACE_EQUAL_TOLERANCE_SECONDS,
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
        return formatPaceSeconds((elapsedMs / 1000.0) / (distanceMeters / 1000.0))
    }

    private fun formatPaceSeconds(paceSecondsPerKm: Double): String {
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${seconds.toString().padStart(2, '0')}″"
    }

    private companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    }
}
