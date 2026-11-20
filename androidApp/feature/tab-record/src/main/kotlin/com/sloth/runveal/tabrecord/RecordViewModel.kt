package com.sloth.runveal.tabrecord

import androidx.lifecycle.viewModelScope
import com.sloth.runveal.data.local.DatabaseDriverFactory
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.db.RunvealDatabase
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.toSnapshotRequest
import com.sloth.runveal.result.TrackResultHolder
import com.sloth.runveal.tabrecord.contract.RecordIntent
import com.sloth.runveal.tabrecord.contract.RecordItem
import com.sloth.runveal.tabrecord.contract.RecordState
import com.sloth.runveal.ui.mvi.MviViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 이번 달을 보고 있을 때 요약 줄 앞머리 — 다른 달이면 "10월"처럼 그 달을 적는다(이 파일의 다른
 * 날짜 라벨과 같이 여기서 만든다). */
private const val THIS_MONTH_LABEL = "이번 달"

/**
 * 기록 탭. 상단 라벨의 화살표로 달을 고를 수 있고(`SelectMonth`), 고른 달은 화면이 살아 있는 동안
 * 유지된다. 기록 삭제는 상세 화면의 더보기 메뉴로 옮겼다.
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
    initialState = RecordState(
        monthLabel = monthLabel(YearMonth.now(zoneId)),
        selectedYear = YearMonth.now(zoneId).year,
        selectedMonth = YearMonth.now(zoneId).monthValue,
    ),
) {
    /** 지금 보고 있는 달. 기본은 이번 달이고, 고른 달이 있으면 그 달이다. */
    private var selectedMonth: YearMonth = YearMonth.now(zoneId)

    init {
        viewModelScope.launch {
            TrackResultHolder.state.collect { load() }
        }
    }

    override suspend fun handleIntent(intent: RecordIntent) {
        when (intent) {
            RecordIntent.Load -> load()
            is RecordIntent.SelectMonth -> {
                selectedMonth = YearMonth.of(intent.year, intent.month)
                load()
            }
        }
    }


    private suspend fun load() {
        val month = selectedMonth
        val from = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val tracks = withContext(Dispatchers.IO) { store.tracksBetween(from, to) }
        val newRoadMeters = withContext(Dispatchers.IO) { store.newRoadMetersBetween(from, to) }
        // 썸네일 경로는 트랙마다 따로 묻지 않고 이 달치를 한 번에 읽는다.
        val trackIds = tracks.map { it.id }
        val routes = withContext(Dispatchers.IO) { store.routesForTracks(trackIds) }
        // 그 러닝이 처음 밟은 구간 — 썸네일에서 "새로 탐험한 길"을 금색으로 덧그린다.
        val newRoads = withContext(Dispatchers.IO) { store.newRoadPathsForTracks(trackIds) }
        // 카드에 "새 길 +1.30km"를 띄우려면 러닝 한 건씩 길이가 필요하다 — 위의 월 합계로는 못 나눈다.
        val newRoadMetersByTrack = withContext(Dispatchers.IO) {
            trackIds.associateWith { store.newRoadMetersForTrack(it) }
        }
        val totalDistanceMeters = tracks.sumOf { it.distanceMeters }

        setState {
            RecordState(
                monthLabel = monthLabel(month),
                selectedYear = month.year,
                selectedMonth = month.monthValue,
                periodLabel = periodLabel(month),
                runCountLabel = "${tracks.size}회",
                totalDistanceValue = "%.2f".format(totalDistanceMeters / 1000.0),
                newRoadValue = "+%.2f".format(newRoadMeters / 1000.0),
                hasNewRoad = newRoadMeters > 0,
                records = tracks.map {
                    toRecordItem(
                        track = it,
                        route = routes[it.id].orEmpty(),
                        newRoadPaths = newRoads[it.id].orEmpty(),
                        newRoadMeters = newRoadMetersByTrack[it.id] ?: 0.0,
                    )
                },
                isLoading = false,
            )
        }
    }

    private fun toRecordItem(
        track: TrackLocalStore.TrackSummary,
        route: List<LatLon>,
        newRoadPaths: List<List<LatLon>>,
        newRoadMeters: Double,
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
            // 처음 간 길이 없는 러닝(같은 길을 또 달린 경우)은 줄 자체를 빼는 게 시안이다.
            newRoadLabel = "새 길 +%.2fkm".format(newRoadMeters / 1000.0).takeIf { newRoadMeters > 0 },
            thumbnailRequest = route.toSnapshotRequest(
                ownerId = track.id,
                variant = SnapshotVariant.THUMBNAIL,
                newRoadPaths = newRoadPaths,
            ),
        )
    }

    /** 요약 줄 앞머리 — 이번 달이면 "이번 달", 다른 달이면 "10월"처럼 그 달을 가리킨다. 해가 다르면
     * 달만 적으면 헷갈려서 연도까지 붙인다. */
    private fun periodLabel(month: YearMonth): String {
        val now = YearMonth.now(zoneId)
        return when {
            month == now -> THIS_MONTH_LABEL
            month.year == now.year -> "${month.monthValue}월"
            else -> "${month.year}년 ${month.monthValue}월"
        }
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
