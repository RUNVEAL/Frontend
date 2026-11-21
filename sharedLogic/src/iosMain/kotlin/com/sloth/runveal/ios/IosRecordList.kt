package com.sloth.runveal.ios

import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant

/** 기록 목록 한 줄 — 전부 화면에 그대로 찍히는 라벨이다. */
data class MonthlyRecordItem(
    val trackId: String,
    val dateLabel: String,
    val courseTitle: String,
    val distanceLabel: String,
    val durationLabel: String,
    val paceLabel: String,
    /** 카드 왼쪽 지도 썸네일. 매칭 경로가 없으면(매칭 실패/점 부족) null이라 플레이스홀더만 보인다. */
    val thumbnailRequest: SnapshotRequest?,
)

/** 기록 탭이 보여줄 이번 달 요약 + 목록. */
data class MonthlyRecordSummary(
    val monthLabel: String,
    /** 지금 보고 있는 달 — 월 선택 패널이 어디에 표시를 둘지 정하는 데 쓴다. */
    val selectedYear: Int,
    val selectedMonth: Int,
    /** 요약 줄 앞머리("이번 달" 또는 "10월") — 보고 있는 달이 이번 달이 아닐 수 있다. */
    val periodLabel: String,
    val runCountLabel: String,
    val totalDistanceValue: String,
    val newRoadValue: String,
    val items: List<MonthlyRecordItem>,
)

/**
 * 기록 탭 데이터 — Android `RecordViewModel`의 DB 조회·포맷 부분에 해당한다.
 *
 * 보고 있는 달은 화면이 들고 있고([load]에 넘긴다), 기본은 이번 달이다.
 */
object IosRecordList {

    private val store by lazy { TrackLocalStore(RunvealDatabaseProvider.database, FileStore()) }

    /** 이번 달 — 화면이 처음 열릴 때. */
    fun loadThisMonth(): MonthlyRecordSummary =
        load(IosDateFormat.currentYear(), IosDateFormat.currentMonth())

    /** 월 선택에서 고른 달. */
    fun load(year: Int, month: Int): MonthlyRecordSummary {
        val (from, to) = IosDateFormat.monthRange(year, month)

        val tracks = store.tracksBetween(from, to)
        val newRoadMeters = store.newRoadMetersBetween(from, to)
        // 썸네일 경로는 트랙마다 따로 묻지 않고 이 달치를 한 번에 읽는다.
        val trackIds = tracks.map { it.id }
        val routes = store.routesForTracks(trackIds)
        // 그 러닝이 처음 밟은 구간 — 썸네일에서 "새로 탐험한 길"을 금색으로 덧그린다.
        val newRoads = store.newRoadPathsForTracks(trackIds)
        val totalDistanceMeters = tracks.sumOf { it.distanceMeters }

        return MonthlyRecordSummary(
            monthLabel = IosDateFormat.monthLabel(year, month),
            selectedYear = year,
            selectedMonth = month,
            periodLabel = IosDateFormat.periodLabel(year, month),
            runCountLabel = "${tracks.size}회",
            totalDistanceValue = formatKilometers(totalDistanceMeters, 2),
            newRoadValue = "+" + formatKilometers(newRoadMeters, 2),
            items = tracks.map { track ->
                MonthlyRecordItem(
                    trackId = track.id,
                    dateLabel = IosDateFormat.monthDayWeekday(track.startedAtEpochMs),
                    // 역지오코딩으로 장소명을 채우기 전까지는 "저녁 러닝"처럼 시간대로 부른다.
                    courseTitle = track.placeName
                        ?: "${IosDateFormat.timeOfDayLabel(track.startedAtEpochMs)} 러닝",
                    distanceLabel = formatKilometers(track.distanceMeters, 2) + "km",
                    durationLabel = IosDateFormat.elapsedLabel(track.elapsedMs),
                    paceLabel = IosDateFormat.paceLabel(track.elapsedMs, track.distanceMeters),
                    thumbnailRequest = IosSnapshot.request(
                        points = routes[track.id].orEmpty(),
                        ownerId = track.id,
                        variant = SnapshotVariant.THUMBNAIL,
                        newRoadPaths = newRoads[track.id].orEmpty(),
                    ),
                )
            },
        )
    }
}
