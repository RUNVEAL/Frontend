package com.swyp.runveal.tabrecord

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.data.local.DatabaseDriverFactory
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.snapshot.SnapshotVariant
import com.swyp.runveal.map.snapshot.toSnapshotRequest
import com.swyp.runveal.result.TrackResultHolder
import com.swyp.runveal.tabrecord.contract.RecordIntent
import com.swyp.runveal.tabrecord.contract.RecordItem
import com.swyp.runveal.tabrecord.contract.RecordState
import com.swyp.runveal.ui.mvi.MviViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 기록 탭. 월 선택·기록 상세는 아직 TODO라, 지금 받는 의도는 기록 삭제뿐이다.
 *
 * 러닝을 끝내면 `TrackingService`가 최종 매칭 후 [TrackResultHolder]에 결과를 publish한다 — 그걸
 * 구독해 그때마다 DB를 다시 읽어, 방금 끝낸 러닝이 목록과 월간 요약에 바로 올라오게 한다. StateFlow라
 * 구독 시작 시 현재 값이 한 번 오므로 화면 진입 시 최초 조회도 이걸로 함께 처리된다(앱을 다시 켜서
 * 홀더가 비어 있어도 null이 한 번 와서 조회가 돈다).
 */
class RecordViewModel(
    private val store: TrackLocalStore = TrackLocalStore(
        RunvealDatabase(DatabaseDriverFactory().createDriver()),
        FileStore(),
    ),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : MviViewModel<RecordIntent, RecordState, Nothing>(
    // 달 라벨은 DB와 무관하니 조회 전부터 보여준다 — 안 그러면 상단바 라벨이 한 프레임 비어 보인다.
    initialState = RecordState(monthLabel = monthLabel(YearMonth.now(zoneId))),
) {

    init {
        viewModelScope.launch {
            TrackResultHolder.state.collect { loadThisMonth() }
        }
    }

    override suspend fun handleIntent(intent: RecordIntent) {
        when (intent) {
            is RecordIntent.DeleteRequested -> setState { copy(deleteTargetTrackId = intent.trackId) }
            RecordIntent.DeleteDismissed -> setState { copy(deleteTargetTrackId = null) }
            RecordIntent.DeleteConfirmed -> deleteTarget()
        }
    }

    /**
     * 기록 한 건을 지운다. [TrackLocalStore.deleteTrack]이 GPX 파일까지 같이 지우고
     * `explored_segment` 커버리지를 남은 기록만으로 다시 만든다 — 그래서 **지운 러닝이 밝혀 둔
     * 길과 탐험률도 같이 줄어든다**. 그 캐시를 다시 만드는 비용이 있어 IO로 넘긴다.
     *
     * 지운 뒤 [TrackResultHolder]를 건드리지 않고 직접 다시 읽는다 — 홀더는 "방금 끝낸 러닝"을
     * 알리는 자리라 삭제와 뜻이 맞지 않는다. 대신 홈 지도는 다음에 오버레이를 다시 만들 때
     * 반영된다.
     */
    private suspend fun deleteTarget() {
        val trackId = currentState.deleteTargetTrackId ?: return
        setState { copy(deleteTargetTrackId = null) }
        withContext(Dispatchers.IO) { store.deleteTrack(trackId, System.currentTimeMillis()) }
        loadThisMonth()
    }

    private suspend fun loadThisMonth() {
        val month = YearMonth.now(zoneId)
        val from = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val tracks = withContext(Dispatchers.IO) { store.tracksBetween(from, to) }
        val newRoadMeters = withContext(Dispatchers.IO) { store.newRoadMetersBetween(from, to) }
        // 썸네일 경로는 트랙마다 따로 묻지 않고 이 달치를 한 번에 읽는다.
        val trackIds = tracks.map { it.id }
        val routes = withContext(Dispatchers.IO) { store.routesForTracks(trackIds) }
        // 그 러닝이 처음 밟은 구간 — 썸네일에서 "새로 탐험한 길"을 금색으로 덧그린다.
        val newRoads = withContext(Dispatchers.IO) { store.newRoadPathsForTracks(trackIds) }
        val totalDistanceMeters = tracks.sumOf { it.distanceMeters }

        setState {
            RecordState(
                monthLabel = monthLabel(month),
                runCountLabel = "${tracks.size}회",
                totalDistanceValue = "%.2f".format(totalDistanceMeters / 1000.0),
                newRoadValue = "+%.2f".format(newRoadMeters / 1000.0),
                records = tracks.map { toRecordItem(it, routes[it.id].orEmpty(), newRoads[it.id].orEmpty()) },
                isLoading = false,
            )
        }
    }

    private fun toRecordItem(
        track: TrackLocalStore.TrackSummary,
        route: List<LatLon>,
        newRoadPaths: List<List<LatLon>>,
    ): RecordItem {
        val startedAt = Instant.ofEpochMilli(track.startedAtEpochMs).atZone(zoneId)
        return RecordItem(
            trackId = track.id,
            dateLabel = "${startedAt.monthValue}월 ${startedAt.dayOfMonth}일 ${dayOfWeekLabel(startedAt)}",
            // 역지오코딩으로 장소명을 채우기 전까지는 "저녁 러닝"처럼 시간대로 부른다.
            courseTitle = track.placeName ?: "${timeOfDayLabel(startedAt)} 러닝",
            distanceLabel = "%.2fkm".format(track.distanceMeters / 1000.0),
            durationLabel = formatElapsed(track.elapsedMs),
            paceLabel = formatPace(track.elapsedMs, track.distanceMeters),
            thumbnailRequest = route.toSnapshotRequest(
                ownerId = track.id,
                variant = SnapshotVariant.THUMBNAIL,
                newRoadPaths = newRoadPaths,
            ),
        )
    }

    private fun dayOfWeekLabel(dateTime: ZonedDateTime): String = when (dateTime.dayOfWeek) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }

    /** `ResultViewModel.formatDate`와 같은 구간 기준. */
    private fun timeOfDayLabel(dateTime: ZonedDateTime): String = when (dateTime.hour) {
        in 5..10 -> "아침"
        in 11..13 -> "점심"
        in 14..17 -> "오후"
        in 18..21 -> "저녁"
        else -> "밤"
    }

    /** 결과 화면과 달리 한 시간을 넘긴 기록도 들어오므로 1시간 이상이면 `h:mm:ss`로 늘린다. */
    private fun formatElapsed(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }

    /** "6'05″" 형태 — km당 분'초″. `ResultViewModel.formatPace`와 같은 공식. */
    private fun formatPace(elapsedMs: Long, distanceMeters: Double): String {
        if (distanceMeters <= 0.0) return "0'00″"
        val paceSecondsPerKm = (elapsedMs / 1000.0) / (distanceMeters / 1000.0)
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${seconds.toString().padStart(2, '0')}″"
    }
}

private fun monthLabel(month: YearMonth): String = "${month.year}년 ${month.monthValue}월"
