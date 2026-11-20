package com.sloth.runveal.ios

import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.dong.region.RegionHierarchyParser
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.recording.RunSplits
import com.sloth.runveal.result.DongRateChanges
import kotlinx.coroutines.Dispatchers
// Kotlin/Native에서 `Dispatchers.IO`는 객체의 멤버가 아니라 **확장 프로퍼티**다(멤버 쪽은 internal).
// 이 import가 없으면 "Cannot access 'val IO': it is internal"로 깨진다 — 지우지 않는다.
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * "구간 기록" 한 줄 — 1km마다 끊은 페이스. [barRatio]는 막대 길이(0~1)로, **빠른 구간일수록 길다**
 * (가장 빠른 구간이 꽉 찬다). [isFastest]면 막대를 강조해 그린다.
 */
data class RecordDetailSplitRow(
    val label: String,
    val paceLabel: String,
    val barRatio: Double,
    val isFastest: Boolean,
)

/** "탐험률 변화" 한 줄 — 이 러닝 직전과 직후의 동네 탐험률(%). */
data class RecordDetailDongRow(
    val name: String,
    val beforePercent: Int,
    val afterPercent: Int,
)

/** 기록 상세에 그대로 찍히는 값들 — 포맷은 전부 여기서 끝낸다(Android `RecordDetailViewModel`). */
data class RecordDetailData(
    val title: String,
    val dateTimeLabel: String,
    val distanceValue: String,
    val newRoadValue: String,
    val elapsedLabel: String,
    val paceLabel: String,
    /** 칼로리는 아직 저장하는 값이 없어 "-"로 둔다 — 자리는 시안대로 지킨다. */
    val caloriesLabel: String,
    val dongStats: List<RecordDetailDongRow>,
    val splits: List<RecordDetailSplitRow>,
    /** 매칭 경로가 없으면(매칭 실패·점 부족) null이라 지도 자리에 플레이스홀더만 보인다. */
    val mapSnapshotRequest: SnapshotRequest?,
)

/**
 * 기록 상세 데이터 — Android `RecordDetailViewModel`의 DB 조회·포맷 부분에 해당한다.
 * 화면 상태(시트·다이얼로그 노출)는 Swift `RecordDetailViewModel`이 들고 있다.
 */
object IosRecordDetail {

    /** 칼로리는 아직 어디에도 저장하지 않는다 — 값이 생기기 전까지 자리만 지킨다. */
    private const val CALORIES_PLACEHOLDER = "-"

    /** 가장 느린 구간이라도 막대가 보이게 두는 최소 길이 비율. */
    private const val MIN_SPLIT_BAR_RATIO = 0.08

    /** 이 차이 이내면 같은 페이스로 본다(km당 초) — 라벨도 막대도 구분이 안 가는 수준이다. */
    private const val PACE_EQUAL_TOLERANCE_SECONDS = 1.0

    private val store by lazy { TrackLocalStore(RunvealDatabaseProvider.database, FileStore()) }

    /**
     * 기록 한 건을 읽어 화면에 찍을 값으로 바꾼다. 없는 기록이면 null — 목록에서 지운 기록을
     * 뒤로가기로 되돌아와 다시 열었을 때가 그렇다.
     *
     * [regionHierarchyJson]은 동 이름을 붙이는 데 쓴다(번들 `korea_region_hierarchy.json`). iOS
     * 번들에서 읽는 건 Swift 몫이라 여기서는 넘겨받은 텍스트만 쓴다([DongBundleAssets]와 같은 규칙).
     */
    @Throws(Exception::class)
    suspend fun load(trackId: String, regionHierarchyJson: String): RecordDetailData? =
        withContext(Dispatchers.IO) {
            val track = store.trackSummary(trackId) ?: return@withContext null

            // 목록 썸네일과 같은 재료를 쓰되 크기만 결과 카드(RESULT_CARD)로 — 같은 경로라도
            // 자리마다 따로 캐시된다.
            val route = store.routesForTracks(listOf(trackId))[trackId].orEmpty()
            val newRoadPaths = store.newRoadPathsForTracks(listOf(trackId))[trackId].orEmpty()
            val newRoadMeters = store.newRoadMetersForTrack(trackId)

            RecordDetailData(
                // 역지오코딩으로 장소명을 채우기 전까지는 "저녁 러닝"처럼 시간대로 부른다(기록 탭과 같은 규칙).
                title = track.placeName
                    ?: "${IosDateFormat.timeOfDayLabel(track.startedAtEpochMs)} 러닝",
                dateTimeLabel = IosDateFormat.dateTimeLabel(track.startedAtEpochMs),
                distanceValue = formatKilometers(track.distanceMeters, 2),
                newRoadValue = "+" + formatKilometers(newRoadMeters, 1),
                elapsedLabel = IosDateFormat.elapsedLabel(track.elapsedMs),
                paceLabel = IosDateFormat.paceLabel(track.elapsedMs, track.distanceMeters),
                caloriesLabel = CALORIES_PLACEHOLDER,
                dongStats = loadDongStats(trackId, regionHierarchyJson),
                splits = loadSplits(trackId),
                mapSnapshotRequest = IosSnapshot.requestForPaths(
                    paths = listOf(route),
                    ownerId = trackId,
                    variant = SnapshotVariant.RESULT_CARD,
                    newRoadPaths = newRoadPaths,
                ),
            )
        }

    /** 앞뒤 공백을 다듬은 이름으로 바꾼다. 빈 이름이면 아무것도 하지 않는다. */
    @Throws(Exception::class)
    suspend fun rename(trackId: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val newName = name.trim()
        if (newName.isEmpty()) return@withContext false
        store.renameTrack(trackId, newName, IosDateFormat.nowEpochMs())
        true
    }

    /**
     * 기록 한 건을 지운다. [TrackLocalStore.deleteTrack]이 GPX 파일까지 같이 지우고
     * `explored_segment` 커버리지를 남은 기록만으로 다시 만든다 — 그래서 **지운 러닝이 밝혀 둔 길과
     * 탐험률도 같이 줄어든다**. 그 캐시를 다시 만드는 비용이 있어 IO로 넘긴다.
     */
    @Throws(Exception::class)
    suspend fun delete(trackId: String) {
        withContext(Dispatchers.IO) { store.deleteTrack(trackId, IosDateFormat.nowEpochMs()) }
    }

    /** GPX 내보내기에 쓰는 원본 — 없으면 null. */
    @Throws(Exception::class)
    suspend fun gpx(trackId: String): String? = withContext(Dispatchers.IO) { store.filteredGpx(trackId) }

    /**
     * 1km 구간별 페이스. 저장해 둔 GPX를 읽어([IosGpxPoints]) 공용 계산([RunSplits])에 넘긴다 —
     * 거리는 `track.distance_meters`가 아니라 점 사이를 다시 재서 쌓으므로 구간 경계가 실제 경로를 따른다.
     *
     * 막대 길이는 **가장 빠른 구간을 1로 둔 비율**이다(느릴수록 짧다). 0에 가까워 안 보이는 일이
     * 없게 최소 길이를 둔다 — 그래서 전 구간 페이스가 같으면 전부 꽉 찬다.
     *
     * 강조(가장 빠른 구간)는 **구간들 사이에 차이가 있을 때만** 켠다 — 다 같은 페이스면 "가장 빠른
     * 구간"이랄 게 없다. 구간이 하나뿐이면 그 줄이 곧 가장 빠른 구간이라 켠다.
     */
    private fun loadSplits(trackId: String): List<RecordDetailSplitRow> {
        val gpx = store.filteredGpx(trackId) ?: return emptyList()
        val splits = RunSplits.of(IosGpxPoints.parse(gpx))
        if (splits.isEmpty()) return emptyList()
        val paces = splits.map { it.paceSecondsPerKm }
        val fastestPace = paces.min().takeIf { it > 0.0 } ?: return emptyList()
        val hasFasterSplit = splits.size == 1 ||
            (paces.max() - fastestPace) > PACE_EQUAL_TOLERANCE_SECONDS
        return splits.map { split ->
            RecordDetailSplitRow(
                label = "${split.index}km",
                paceLabel = IosDateFormat.paceSecondsLabel(split.paceSecondsPerKm),
                barRatio = (fastestPace / split.paceSecondsPerKm)
                    .coerceIn(MIN_SPLIT_BAR_RATIO, 1.0),
                isFastest = hasFasterSplit &&
                    (split.paceSecondsPerKm - fastestPace) <= PACE_EQUAL_TOLERANCE_SECONDS,
            )
        }
    }

    /**
     * 그 러닝이 지난 동들의 탐험률 전/후. 분자는 기록 재생([TrackLocalStore.dongWalkedAroundTrack]),
     * 분모는 구간 인덱스 — 러닝 결과 화면과 **같은 공식**을 쓰려고 [DongRateChanges]를 거친다.
     */
    private fun loadDongStats(trackId: String, regionHierarchyJson: String): List<RecordDetailDongRow> {
        val walked = store.dongWalkedAroundTrack(trackId)
        if (walked.isEmpty()) return emptyList()
        return DongRateChanges.build(
            touchedAdmCodes = walked.map { it.admCd2 }.toSet(),
            beforeWalked = walked.associate { it.admCd2 to it.beforeMeters },
            afterWalked = walked.associate { it.admCd2 to it.afterMeters },
            totalLengths = store.totalLengthByDong(),
            namesByCode = dongNames(regionHierarchyJson),
        ).map {
            RecordDetailDongRow(
                name = it.name,
                beforePercent = it.beforePercent,
                afterPercent = it.afterPercent,
            )
        }
    }

    /** 이름을 못 읽으면 탐험률 줄만 비운다 — 나머지 기록은 그대로 보여준다. */
    private fun dongNames(regionHierarchyJson: String): Map<String, String> =
        try {
            RegionHierarchyParser.parse(regionHierarchyJson).sido
                .flatMap { sido -> sido.sigungu.flatMap { it.dongs } }
                .associate { it.code to it.name }
        } catch (_: Exception) {
            emptyMap()
        }
}
