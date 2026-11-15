package com.swyp.runveal.data.local

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val MATCH_STATE_MATCHED = "MATCHED"

/** `/meta` manifest `matchDistanceM` 기본값(`docs/api/API.md` §3.1.2). */
private const val DEFAULT_MATCH_DISTANCE_M = 15.0

/** [TrackLocalStore.recordMatchResult] 결과 요약 — 러닝 결과 화면(§새로 탐험한 길, 동별 완주율 변화)이
 * 바로 쓸 수 있게 "이번에 처음 걸은 구간"만 집계해서 돌려준다. */
data class MatchRecordSummary(
    val newRoadMeters: Double,
    val touchedDongAdmCodes: Set<String>,
    /** 이번에 처음 걸은 구간들의 경로(구간마다 한 줄) — 결과 카드에서 "처음 간 길"만 금색으로
     * 덧그리는 데 쓴다. 적립이 끝나면 `explored_segment`에 들어가 버려서 나중엔 다시 가려낼 수 없으니
     * 여기서 같이 돌려준다. */
    val newRoadPaths: List<List<LatLon>>,
)

/**
 * DB([RunvealDatabase]) + 파일([FileStore])을 조합하는 리포지토리(docs/local-db/local-db-schema-design.md
 * §2, §5). 테이블 하나짜리 단순 CRUD는 호출부가 `database.xxxQueries`를 직접 쓰면 되고, 이 클래스는
 * DB와 파일을 같이 건드려야 하는 작업([saveTrack], [deleteTrack])과 완주율 집계만 책임진다.
 *
 * "가본 길" 인정 여부는 서버 매칭이 돌려주는 `segmentId`(`docs/api/API.md` §3.6.2) 기준이다 — 구간
 * 길이·행정동은 미리 로컬에 캐시해 둔 `road_segment` 인덱스([com.swyp.runveal.core.map.RoadSegmentIndexImporter])
 * 에서 찾는다. `run_segment`(트랙별 구간, 진실의 원천) → `explored_segment`(구간별 누적, 캐시) 두
 * 단계로 나눠서, 트랙을 지워도 [regenerateCoverage]로 캐시를 다시 만들 수 있다.
 */
class TrackLocalStore(
    val database: RunvealDatabase,
    private val fileStore: FileStore,
) {
    private val json = Json

    /** 기록 종료 시 호출(§5): GPX 파일을 먼저 쓰고, 성공해야 `track` row를 insert한다 — 반대 순서면
     * "DB엔 있는데 파일 없음" 상태가 더 자주 생긴다. */
    fun saveTrack(
        id: String,
        startedAtEpochMs: Long,
        finishedAtEpochMs: Long,
        distanceMeters: Double,
        pointCountFiltered: Int,
        pointCountRaw: Int,
        filteredGpx: String,
        rawGpx: String,
        nowEpochMs: Long,
    ) {
        val filteredPath = "tracks/$id.gpx"
        val rawPath = "tracks/${id}_raw.gpx"
        fileStore.write(filteredPath, filteredGpx)
        fileStore.write(rawPath, rawGpx)
        database.trackQueries.insertTrack(
            id = id,
            started_at_epoch_ms = startedAtEpochMs,
            finished_at_epoch_ms = finishedAtEpochMs,
            distance_meters = distanceMeters,
            point_count_filtered = pointCountFiltered.toLong(),
            point_count_raw = pointCountRaw.toLong(),
            match_state = "NOT_MATCHED",
            confidence_score = null,
            unmatched_point_count = null,
            place_name = null,
            place_lookup_attempted = 0,
            gpx_filtered_path = filteredPath,
            gpx_raw_path = rawPath,
            created_at_epoch_ms = nowEpochMs,
            updated_at_epoch_ms = nowEpochMs,
        )
    }

    /** 매칭 완료 시 호출. [matchedPaths]를 `track_edge`에 조각별로 저장하고(가본 길 렌더링용),
     * [segmentIds] 중 [road_segment] 인덱스에 있는 것만 `run_segment`/`explored_segment`에
     * 적립한다(인덱스에 없으면 — 아직 import 전이거나 알 수 없는 구간이면 — 조용히 건너뛴다).
     *
     * [matchedPaths]가 **조각 목록**인 이유: 서버 매칭 경로에서 실제로 달린 자리와 동떨어진 구간을
     * 잘라내면([com.swyp.runveal.match.MatchedPathFilter]) 선이 여러 조각으로 나뉜다. 그 조각들을
     * 다시 하나로 이으면 잘라낸 구간을 직선으로 바꿔 그릴 뿐이라, 따로 저장하고 따로 그린다.
     *
     * 반환값은 "이번에 전역에서 처음 채택된" 구간만 집계한 [MatchRecordSummary] — 러닝 결과 화면용.
     * 한 트랜잭션이라 도중에 실패하면 전부 롤백된다. */
    fun recordMatchResult(
        trackId: String,
        matchedPaths: List<List<LatLon>>,
        segmentIds: List<Long>,
        nowEpochMs: Long,
    ): MatchRecordSummary {
        var newRoadMeters = 0.0
        val touchedDongAdmCodes = mutableSetOf<String>()
        val newRoadPaths = mutableListOf<List<LatLon>>()
        database.transaction {
            matchedPaths.filter { it.size >= 2 }.forEachIndexed { index, path ->
                database.trackEdgeQueries.insertEdge(
                    track_id = trackId,
                    seq = index.toLong(),
                    coordinates = encodeCoordinates(path),
                    length_meters = GeoMath.totalDistanceMeters(path),
                )
            }
            segmentIds.distinct().forEach { segmentId ->
                database.runSegmentQueries.insertRunSegment(trackId, segmentId)
                adoptSegment(segmentId) { info ->
                    newRoadMeters += info.length_m
                    touchedDongAdmCodes += info.adm_cd2
                    newRoadPaths += RoadSegmentParser.decodeLines(info.coordinates)
                }
            }
            database.trackQueries.updateMatchState(
                match_state = MATCH_STATE_MATCHED,
                confidence_score = 1.0,
                unmatched_point_count = 0,
                updated_at_epoch_ms = nowEpochMs,
                id = trackId,
            )
        }
        return MatchRecordSummary(newRoadMeters, touchedDongAdmCodes, newRoadPaths)
    }

    /** [segmentId]가 [road_segment] 인덱스에 있고, 전역에서 처음 채택되는 경우에만 [onAdopted]를
     * 부른다(`INSERT OR IGNORE` + `changes()`로 실제 insert됐는지 확인 — 같은 트랜잭션 안에서 바로
     * 이어 호출해야 한다). 인덱스에 없는 구간(아직 import 전)은 그냥 무시한다. */
    private inline fun adoptSegment(segmentId: Long, onAdopted: (RoadSegmentInfo) -> Unit): Boolean {
        val info = database.roadSegmentIndexQueries.selectById(segmentId).executeAsOneOrNull() ?: return false
        database.exploredSegmentQueries.insertSegmentIfAbsent(info.segment_id, info.adm_cd2, info.length_m)
        val adopted = database.exploredSegmentQueries.changes().executeAsOne() > 0
        if (adopted) onAdopted(RoadSegmentInfo(info.adm_cd2, info.length_m, info.coordinates))
        return adopted
    }

    private data class RoadSegmentInfo(val adm_cd2: String, val length_m: Double, val coordinates: String)

    /**
     * 달린 선이 **올라탄 구간**들의 ID — 탐험률(`explored_segment`)에 적립할 대상이다.
     *
     * 서버가 준 `segmentIds`를 쓰지 않는다. 서버는 도로망을 따라 경로를 메우기 때문에 안 지난 도로의
     * 구간까지 섞여 오고, 반대로 로컬 인덱스에 없는 ID를 줘서 아무것도 적립 안 되는 경우도 생긴다.
     * 대신 러닝 중 흰색/금색을 가르는 것과 **같은 규칙**([RoadSegmentLocator] — 점에서 가장 가까운
     * 구간)으로 직접 고른다. 그래야 "러닝 중엔 새 길이었는데 탐험률은 안 움직이는" 어긋남이 없다.
     */
    fun segmentIdsOnPaths(paths: List<List<LatLon>>, matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M): List<Long> {
        val locator = RoadSegmentLocator(database, matchDistanceM)
        val ids = linkedSetOf<Long>()
        paths.forEach { path -> path.forEach { point -> locator.nearestSegmentId(point)?.let(ids::add) } }
        return ids.toList()
    }

    /** 기록 탭 한 줄치 요약 — 라벨 포맷은 화면(ViewModel)이 맡고, 여기선 숫자/시각만 넘긴다. */
    data class TrackSummary(
        val id: String,
        val startedAtEpochMs: Long,
        val elapsedMs: Long,
        val distanceMeters: Double,
        /** 역지오코딩으로 채우는 장소명 — 아직 조회 전/실패면 null이라 화면이 대체 제목을 쓴다. */
        val placeName: String?,
    )

    /** 기록 탭 목록: `[fromEpochMs, toEpochMs)` 안에 *시작한* 트랙을 최신순으로. 러닝 횟수/총 거리는
     * 이 목록만으로 계산되므로 따로 집계 쿼리를 두지 않는다. */
    fun tracksBetween(fromEpochMs: Long, toEpochMs: Long): List<TrackSummary> =
        database.trackQueries.selectStartedBetweenDesc(fromEpochMs = fromEpochMs, toEpochMs = toEpochMs)
            .executeAsList()
            .map { row ->
                TrackSummary(
                    id = row.id,
                    startedAtEpochMs = row.started_at_epoch_ms,
                    elapsedMs = row.finished_at_epoch_ms - row.started_at_epoch_ms,
                    distanceMeters = row.distance_meters,
                    placeName = row.place_name,
                )
            }

    /** 그 기간에 처음 달린 구간의 길이 합(m) — 기록 탭 월간 "새로 탐험한 길". */
    fun newRoadMetersBetween(fromEpochMs: Long, toEpochMs: Long): Double =
        database.runSegmentQueries.newRoadMetersBetween(fromEpochMs = fromEpochMs, toEpochMs = toEpochMs)
            .executeAsOne()

    /** 기록 탭 썸네일용 트랙별 매칭 경로. 매칭이 아직/끝내 안 된 트랙은 `track_edge`가 비어 있어서
     * 결과 맵에 키가 없다 — 호출부는 그때 썸네일 없이(플레이스홀더) 그린다. */
    fun routesForTracks(trackIds: List<String>): Map<String, List<LatLon>> {
        if (trackIds.isEmpty()) return emptyMap()
        return database.trackEdgeQueries.selectEdgesForTracks(trackIds).executeAsList()
            .groupBy { it.track_id }
            .mapValues { (_, rows) -> rows.flatMap { decodeCoordinates(it.coordinates) } }
    }

    /**
     * [trackIds] 각각이 **처음 밟은** 구간들의 경로(구간마다 한 줄). 결과 카드·기록 썸네일에서 "새로
     * 탐험한 길"을 금색으로 덧그리는 데 쓴다 — 같은 길을 또 달린 러닝은 그만큼 비어 있다.
     *
     * 러닝이 끝나는 순간에는 [recordMatchResult]가 돌려주지만, 지나고 나면 `explored_segment`엔
     * "지금 가봤는가"만 남아 어느 러닝이 그 길을 뚫었는지 모른다 — 그래서 `run_segment`를 트랙 시작
     * 시각으로 되짚어 가린다.
     */
    fun newRoadPathsForTracks(trackIds: List<String>): Map<String, List<List<LatLon>>> {
        if (trackIds.isEmpty()) return emptyMap()
        return database.runSegmentQueries.selectFirstRunSegmentGeometries(trackIds).executeAsList()
            .groupBy { it.track_id }
            .mapValues { (_, rows) -> rows.flatMap { RoadSegmentParser.decodeLines(it.coordinates) } }
    }

    /** 동별 완주 길이(분자) — `explored_segment`를 바로 집계한다. */
    fun walkedLengthByDong(): Map<String, Double> =
        database.exploredSegmentQueries.walkedLengthByDong().executeAsList()
            .associate { it.adm_cd2 to (it.walked_meters ?: 0.0) }

    /** 동별 전체 도로 길이(분모) — `road_segment` 인덱스를 바로 집계한다(`RoadSegmentIndexImporter`가
     * 아직 안 들여왔으면 비어 있다). */
    fun totalLengthByDong(): Map<String, Double> =
        database.roadSegmentIndexQueries.totalLengthByDong().executeAsList()
            .associate { it.adm_cd2 to (it.total_meters ?: 0.0) }

    /** 가본 길 렌더링(§3.2): `track_edge` row 하나가 Feature 하나다.
     *
     * 예전엔 트랙별로 합쳐 Feature 1개로 만들었다(조각 경계마다 glow round cap이 겹쳐 빛이 번지는
     * 문제 때문). 지금은 한 트랙의 조각들이 **실제로 떨어져 있는** 구간이라 — 매칭 경로에서 GPS와
     * 동떨어진 부분을 잘라낸 결과([com.swyp.runveal.match.MatchedPathFilter]) — 합치면 안 지난 길을
     * 직선으로 이어 그리게 된다. */
    fun visitedRoadEdges(): List<MatchedEdge> =
        database.trackEdgeQueries.selectAllEdgesForRendering().executeAsList()
            .map { MatchedEdge(coordinates = decodeCoordinates(it.coordinates)) }

    /** 트랙 삭제(§5): DB row 삭제(→ edge/note/run_segment CASCADE) → 완주율 캐시 재생성 → GPX 파일
     * 삭제. 중간에 실패해도 고아 GPX만 남고 기능엔 영향 없다. */
    fun deleteTrack(id: String) {
        val track = database.trackQueries.selectById(id).executeAsOneOrNull() ?: return
        database.trackQueries.deleteTrack(id)
        regenerateCoverage()
        fileStore.delete(track.gpx_filtered_path)
        fileStore.delete(track.gpx_raw_path)
    }

    /** `explored_segment` 캐시를 비우고 `run_segment`(남은 트랙 전체)를 다시 돌려 채운다 — segment_id가
     * PK라 순서와 무관하게 결과가 같다(멱등). */
    fun regenerateCoverage() {
        database.transaction {
            database.exploredSegmentQueries.clearExploredSegments()
            database.runSegmentQueries.selectAllForRebuild().executeAsList().forEach { row ->
                val info = database.roadSegmentIndexQueries.selectById(row.segment_id).executeAsOneOrNull() ?: return@forEach
                database.exploredSegmentQueries.insertSegmentIfAbsent(info.segment_id, info.adm_cd2, info.length_m)
            }
        }
    }

    private fun encodeCoordinates(coordinates: List<LatLon>): String =
        json.encodeToString(
            ListSerializer(ListSerializer(Double.serializer())),
            coordinates.map { listOf(it.lon, it.lat) },
        )

    private fun decodeCoordinates(text: String): List<LatLon> =
        json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), text)
            .map { LatLon(lat = it[1], lon = it[0]) }
}
