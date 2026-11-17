package com.sloth.runveal.data.local

import com.sloth.runveal.db.RunvealDatabase
import com.sloth.runveal.dong.CoverageMask
import com.sloth.runveal.dong.MatchedEdge
import com.sloth.runveal.dong.SegmentCoverageMatcher
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val MATCH_STATE_MATCHED = "MATCHED"

/** `/meta` manifest `matchDistanceM` 기본값(`docs/api/API.md` §3.1.2). */
private const val DEFAULT_MATCH_DISTANCE_M = 15.0

/** 구간 하나에서 **지나간 부분** — [TrackLocalStore.coverageOnPaths]가 고르고
 * [TrackLocalStore.recordMatchResult]가 적립한다. */
data class SegmentCoverage(val segmentId: Long, val mask: CoverageMask)

/** [TrackLocalStore.recordMatchResult] 결과 요약 — 러닝 결과 화면(§새로 탐험한 길, 동별 완주율 변화)이
 * 바로 쓸 수 있게 "이번에 처음 지나간 부분"만 집계해서 돌려준다. */
data class MatchRecordSummary(
    val newRoadMeters: Double,
    val touchedDongAdmCodes: Set<String>,
    /** 이번에 처음 지나간 부분의 경로(토막마다 한 줄) — 결과 카드에서 "처음 간 길"만 금색으로
     * 덧그리는 데 쓴다. 적립이 끝나면 `explored_segment`에 합쳐져서 나중엔 다시 가려낼 수 없으니
     * 여기서 같이 돌려준다. */
    val newRoadPaths: List<List<LatLon>>,
)

/**
 * DB([RunvealDatabase]) + 파일([FileStore])을 조합하는 리포지토리(docs/local-db/local-db-schema-design.md
 * §2, §5). 테이블 하나짜리 단순 CRUD는 호출부가 `database.xxxQueries`를 직접 쓰면 되고, 이 클래스는
 * DB와 파일을 같이 건드려야 하는 작업([saveTrack], [deleteTrack])과 완주율 집계만 책임진다.
 *
 * "가본 길" 인정은 달린 선이 로컬 `road_segment` 인덱스의 도로선 위를 지났는지로 판정한다
 * ([coverageOnPaths]) — 구간 전체가 아니라 **지나간 부분만**([com.sloth.runveal.dong.CoverageMask]).
 * `run_segment`(트랙별 커버리지, 진실의 원천) → `explored_segment`(구간별 누적, 캐시) 두 단계로
 * 나눠서, 트랙을 지워도 [regenerateCoverage]로 캐시를 다시 만들 수 있다.
 */
class TrackLocalStore(
    val database: RunvealDatabase,
    private val fileStore: FileStore,
) {
    private val json = Json

    /** 기록 종료 시 호출(§5): GPX 파일을 먼저 쓰고, 성공해야 `track` row를 insert한다 — 반대 순서면
     * "DB엔 있는데 파일 없음" 상태가 더 자주 생긴다.
     *
     * [source]/[externalId]는 외부 기록을 가져올 때만 채운다(§3.7) — [externalId]에는 UNIQUE 제약이
     * 걸려 있어서, 같은 외부 기록을 두 번 넣으려 하면 insert가 실패한다(중복 방지를 DB가 보장). */
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
        source: TrackSource = TrackSource.LOCAL,
        externalId: String? = null,
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
            source = source.name,
            external_id = externalId,
        )
    }

    /** 이미 들여온 외부 기록 ID — 가져오기 시작 전에 한 번 읽어 건너뛸 세션을 걸러낸다. */
    fun importedExternalIds(source: TrackSource): Set<String> =
        database.trackQueries.selectExternalIdsBySource(source.name)
            .executeAsList()
            .mapNotNull { it }
            .toSet()

    /** 매칭 완료 시 호출. [matchedPaths]를 `track_edge`에 조각별로 저장하고(가본 길 렌더링용),
     * [coverages] 중 [road_segment] 인덱스에 있는 것만 `run_segment`/`explored_segment`에
     * 적립한다(인덱스에 없으면 — 아직 import 전이거나 알 수 없는 구간이면 — 조용히 건너뛴다).
     *
     * 적립은 구간 단위가 아니라 **샘플 단위**다 — 300m 구간을 20m만 달렸으면 그 20m만 쌓인다. 이미
     * 쌓인 커버리지와 OR로 합치므로, 나머지를 나중에 달리면 그때 이어서 채워진다.
     *
     * [matchedPaths]가 **조각 목록**인 이유: 서버 매칭 경로에서 실제로 달린 자리와 동떨어진 구간을
     * 잘라내면([com.sloth.runveal.match.MatchedPathFilter]) 선이 여러 조각으로 나뉜다. 그 조각들을
     * 다시 하나로 이으면 잘라낸 구간을 직선으로 바꿔 그릴 뿐이라, 따로 저장하고 따로 그린다.
     *
     * 반환값은 "이번에 전역에서 처음 채택된" 구간만 집계한 [MatchRecordSummary] — 러닝 결과 화면용.
     * 한 트랜잭션이라 도중에 실패하면 전부 롤백된다. */
    fun recordMatchResult(
        trackId: String,
        matchedPaths: List<List<LatLon>>,
        coverages: List<SegmentCoverage>,
        nowEpochMs: Long,
    ): MatchRecordSummary {
        val geometry = SegmentGeometry(database)
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
            coverages.forEach { coverage ->
                val info = geometry.info(coverage.segmentId) ?: return@forEach
                val sampleLines = geometry.sampleLines(coverage.segmentId)
                val sampleCount = sampleLines.sumOf { it.size }
                if (sampleCount == 0) return@forEach
                val mask = coverage.mask.resizedTo(sampleCount)
                if (mask.isEmpty()) return@forEach
                database.runSegmentQueries.insertRunSegment(trackId, coverage.segmentId, mask.encode())

                // 지금까지 쌓인 커버리지와 합치고(OR), 이번에 **새로** 덮인 부분만 결과 화면에 돌려준다.
                val storedMask = database.exploredSegmentQueries
                    .selectCoverageForSegment(coverage.segmentId).executeAsOneOrNull()
                val accumulated = storedMask?.let { CoverageMask.decode(it).resizedTo(sampleCount) }
                val merged = accumulated?.or(mask) ?: mask
                val metersPerSample = info.lengthM / sampleCount
                val coveredM = merged.coveredCount * metersPerSample
                if (accumulated == null) {
                    database.exploredSegmentQueries.insertSegment(
                        coverage.segmentId, info.admCd2, info.lengthM, merged.encode(), coveredM,
                    )
                } else {
                    database.exploredSegmentQueries.updateCoverage(
                        coveredMask = merged.encode(),
                        coveredM = coveredM,
                        segmentId = coverage.segmentId,
                    )
                }

                val added = accumulated?.let { mask.andNot(it) } ?: mask
                if (added.isEmpty()) return@forEach
                newRoadMeters += added.coveredCount * metersPerSample
                touchedDongAdmCodes += info.admCd2
                newRoadPaths += SegmentCoverageMatcher.coveredSubPaths(sampleLines, added)
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

    /** 구간 하나의 길이·행정동·샘플 점을 한 번만 읽어 들고 있는 조회 캐시 — 같은 구간을 여러 번
     * 묻기 때문이다(구간마다 MultiLineString 디코딩 + 샘플링이라 다시 하면 비싸다). */
    private class SegmentGeometry(private val database: RunvealDatabase) {
        private val locator = RoadSegmentLocator(database)
        private val infos = mutableMapOf<Long, Info>()

        data class Info(val admCd2: String, val lengthM: Double)

        /** `road_segment` 인덱스에 없으면 null — 길이·동을 몰라 적립도 집계도 못 한다. */
        fun info(segmentId: Long): Info? = infos.getOrPut(segmentId) {
            val row = database.roadSegmentIndexQueries.selectById(segmentId).executeAsOneOrNull()
                ?: return null
            Info(admCd2 = row.adm_cd2, lengthM = row.length_m)
        }

        fun sampleLines(segmentId: Long): List<List<LatLon>> = locator.samplePointsByLine(segmentId)
    }

    /**
     * 달린 선이 **올라탄 구간**과 그 구간에서 **지나간 부분** — 탐험률에 적립할 대상이다.
     *
     * 두 단계로 거른다:
     *  1. 구간 고르기 — 점마다 **가장 가까운 구간 하나**([RoadSegmentLocator]). 러닝 중 흰색/금색을
     *     가르는 것과 같은 규칙이라 "러닝 중엔 새 길이었는데 탐험률은 안 움직이는" 어긋남이 없다.
     *  2. 부분 고르기 — 고른 구간에서 달린 선 [matchDistanceM] 안에 있는 샘플만. 구간 전체를 통째로
     *     적립하면 300m 도로를 20m 밟고 지나가도 300m를 다 탐험한 것이 된다.
     *
     * 서버가 준 `segmentIds`는 쓰지 않는다. 서버는 도로망을 따라 경로를 메우기 때문에 안 지난 도로의
     * 구간까지 섞여 오고, 반대로 로컬 인덱스에 없는 ID를 줘서 아무것도 적립 안 되는 경우도 생긴다.
     */
    fun coverageOnPaths(
        paths: List<List<LatLon>>,
        matchDistanceM: Double = DEFAULT_MATCH_DISTANCE_M,
    ): List<SegmentCoverage> {
        val locator = RoadSegmentLocator(database, matchDistanceM)
        val candidates = linkedSetOf<Long>()
        paths.forEach { path -> path.forEach { point -> locator.nearestSegmentId(point)?.let(candidates::add) } }
        return candidates.map { segmentId ->
            SegmentCoverage(
                segmentId = segmentId,
                mask = SegmentCoverageMatcher.coverageMask(
                    samples = locator.samplePoints(segmentId),
                    referencePaths = paths,
                    matchDistanceM = matchDistanceM,
                ),
            )
        }
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

    /** 그 기간에 **처음 지나간 부분**의 길이 합(m) — 기록 탭 월간 "새로 탐험한 길". 구간을 쪼개
     * 적립하므로, 지난달에 절반 달린 구간을 이달에 끝까지 달리면 나머지 절반만 이달에 들어간다. */
    fun newRoadMetersBetween(fromEpochMs: Long, toEpochMs: Long): Double {
        var meters = 0.0
        replayRunSegments { delta ->
            if (delta.startedAtEpochMs >= fromEpochMs && delta.startedAtEpochMs < toEpochMs) {
                meters += delta.addedMeters
            }
        }
        return meters
    }

    /** 기록 탭 썸네일용 트랙별 매칭 경로. 매칭이 아직/끝내 안 된 트랙은 `track_edge`가 비어 있어서
     * 결과 맵에 키가 없다 — 호출부는 그때 썸네일 없이(플레이스홀더) 그린다. */
    fun routesForTracks(trackIds: List<String>): Map<String, List<LatLon>> {
        if (trackIds.isEmpty()) return emptyMap()
        return database.trackEdgeQueries.selectEdgesForTracks(trackIds).executeAsList()
            .groupBy { it.track_id }
            .mapValues { (_, rows) -> rows.flatMap { decodeCoordinates(it.coordinates) } }
    }

    /**
     * [trackIds] 각각이 **처음 지나간 부분**의 경로. 결과 카드·기록 썸네일에서 "새로 탐험한 길"을
     * 금색으로 덧그리는 데 쓴다 — 같은 길을 또 달린 러닝은 그만큼 비어 있고, 구간을 쪼개 적립하므로
     * 그 러닝이 실제로 지난 토막만 나온다.
     *
     * 러닝이 끝나는 순간에는 [recordMatchResult]가 돌려주지만, 지나고 나면 `explored_segment`엔
     * 누적만 남아 어느 러닝이 그 길을 뚫었는지 모른다 — 그래서 `run_segment`를 트랙 시작 시각으로
     * 되짚어 가린다.
     */
    fun newRoadPathsForTracks(trackIds: List<String>): Map<String, List<List<LatLon>>> {
        if (trackIds.isEmpty()) return emptyMap()
        val wanted = trackIds.toSet()
        val geometry = SegmentGeometry(database)
        val paths = mutableMapOf<String, MutableList<List<LatLon>>>()
        replayRunSegments { delta ->
            if (delta.trackId in wanted) {
                // 선을 그릴 때만 지오메트리가 필요하다 — mask 해상도를 지금 샘플 수에 맞춰 놓고 센다.
                val sampleLines = geometry.sampleLines(delta.segmentId)
                val subPaths = SegmentCoverageMatcher.coveredSubPaths(
                    sampleLines = sampleLines,
                    mask = delta.added.resizedTo(sampleLines.sumOf { it.size }),
                )
                if (subPaths.isNotEmpty()) paths.getOrPut(delta.trackId) { mutableListOf() } += subPaths
            }
        }
        return paths
    }

    /** `run_segment`를 트랙 시작 시각 순으로 재생한다 — [onNewlyCovered]는 그 트랙이 그 시점까지의
     * 누적 대비 **새로 덮은 부분**만 받는다. 반환값은 전부 합친 구간별 커버리지(캐시 재생성용).
     *
     * 시간순이어야 하는 이유: "이번에 처음 덮인 부분"은 앞선 트랙들을 합친 결과에 따라 달라진다. */
    private fun replayRunSegments(
        onNewlyCovered: (RunSegmentDelta) -> Unit = {},
    ): Map<Long, ReplayedSegment> {
        val accumulated = mutableMapOf<Long, ReplayedSegment>()
        database.runSegmentQueries.selectAllForReplay().executeAsList().forEach { row ->
            val decoded = CoverageMask.decode(row.covered_mask)
            val before = accumulated[row.segment_id]
            // 해상도(샘플 수)는 둘 중 촘촘한 쪽에 맞춘다 — 지도 버전이 바뀌어 샘플 수가 달라진
            // 기록이 섞여 있어도 적립을 깎지 않고, 읽는 순서에 따라 결과가 달라지지도 않는다.
            val size = maxOf(decoded.size, before?.mask?.size ?: 0)
            val mask = decoded.resizedTo(size)
            val accumulatedMask = before?.mask?.resizedTo(size) ?: CoverageMask.empty(size)
            accumulated[row.segment_id] = ReplayedSegment(
                mask = accumulatedMask.or(mask),
                admCd2 = row.adm_cd2,
                lengthM = row.length_m,
            )
            val added = mask.andNot(accumulatedMask)
            if (added.isEmpty()) return@forEach
            onNewlyCovered(
                RunSegmentDelta(
                    trackId = row.track_id,
                    startedAtEpochMs = row.started_at_epoch_ms,
                    segmentId = row.segment_id,
                    added = added,
                    addedMeters = added.coveredCount * (row.length_m / size),
                ),
            )
        }
        return accumulated
    }

    /** 재생 결과 — 구간별 누적 커버리지와 길이 환산에 필요한 값. */
    private data class ReplayedSegment(val mask: CoverageMask, val admCd2: String, val lengthM: Double)

    private data class RunSegmentDelta(
        val trackId: String,
        val startedAtEpochMs: Long,
        val segmentId: Long,
        val added: CoverageMask,
        /** [added]를 길이로 환산한 값(m). */
        val addedMeters: Double,
    )

    /** 동별 완주 길이(분자) — `explored_segment`를 바로 집계한다. */
    /** 지금까지 달린 거리 전부(m). 기록이 없으면 0. */
    fun totalDistanceMeters(): Double =
        database.trackQueries.totalDistanceMeters().executeAsOne().total_meters ?: 0.0

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
     * 동떨어진 부분을 잘라낸 결과([com.sloth.runveal.match.MatchedPathFilter]) — 합치면 안 지난 길을
     * 직선으로 이어 그리게 된다. */
    fun visitedRoadEdges(): List<MatchedEdge> =
        database.trackEdgeQueries.selectAllEdgesForRendering().executeAsList()
            .map { MatchedEdge(coordinates = decodeCoordinates(it.coordinates)) }

    /** 트랙 삭제(§5): DB row 삭제(→ edge/note/run_segment CASCADE) → 완주율 캐시 재생성 → GPX 파일
     * 삭제. 중간에 실패해도 고아 GPX만 남고 기능엔 영향 없다. */
    /**
     * 기록을 목록에서 지운다 — **소프트 삭제**다(행은 남는다).
     *
     * 그 러닝이 밝힌 길은 지도에 그대로 남아야 한다. 행을 진짜로 지우면 `run_segment`·`track_edge`가
     * ON DELETE CASCADE로 같이 날아가, 적립(탐험률)과 "가본 길" 선까지 사라진다. 그래서 지운 시각만
     * 적고 목록 쿼리에서만 걸러낸다 — [regenerateCoverage]도 부르지 않는다(부르면 그 트랙 몫이
     * 빠진 캐시가 다시 만들어진다).
     *
     * GPX 파일도 남겨 둔다. 되살리거나 다시 매칭할 여지를 없애지 않기 위해서다.
     *
     * TODO: 지운 기록이 쌓이기만 한다. 되살리기(실행 취소)를 붙이거나 오래된 것을 실제로 정리하는
     *  규칙이 필요하다 — 그때는 적립을 어떻게 유지할지 같이 정해야 한다.
     */
    fun deleteTrack(id: String, nowEpochMs: Long) {
        database.trackQueries.softDeleteTrack(
            deleted_at_epoch_ms = nowEpochMs,
            updated_at_epoch_ms = nowEpochMs,
            id = id,
        )
    }

    /** `explored_segment` 캐시를 비우고 `run_segment`(남은 트랙 전체)의 커버리지를 다시 합쳐 채운다 —
     * 트랙별 mask를 OR하는 것뿐이라 순서와 무관하게 결과가 같다(멱등). */
    fun regenerateCoverage() {
        val coverage = replayRunSegments()
        database.transaction {
            database.exploredSegmentQueries.clearExploredSegments()
            coverage.forEach { (segmentId, segment) ->
                database.exploredSegmentQueries.insertSegment(
                    segmentId,
                    segment.admCd2,
                    segment.lengthM,
                    segment.mask.encode(),
                    segment.mask.coveredCount * (segment.lengthM / segment.mask.size),
                )
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
