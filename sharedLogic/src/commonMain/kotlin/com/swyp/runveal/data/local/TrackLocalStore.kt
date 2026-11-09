package com.swyp.runveal.data.local

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongCoverageCalculator
import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val MATCH_STATE_MATCHED = "MATCHED"

/** [TrackLocalStore.recordMatchResult] 결과 요약 — 러닝 결과 화면(§새로 탐험한 길, 동별 완주율 변화)이
 * 바로 쓸 수 있게 "이번에 처음 걸은 길"만 집계해서 돌려준다. */
data class MatchRecordSummary(
    val newRoadMeters: Double,
    val touchedDongAdmCodes: Set<String>,
)

/**
 * DB([RunvealDatabase]) + 파일([FileStore])을 조합하는 리포지토리(docs/local-db/local-db-schema-design.md
 * §2, §5). 테이블 하나짜리 단순 CRUD는 호출부가 `database.xxxQueries`를 직접 쓰면 되고, 이 클래스는
 * DB와 파일을 같이 건드려야 하는 작업([saveTrack], [deleteTrack])과 §3.3의 완주율 집계 알고리즘만
 * 책임진다.
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

    /** 매칭(Valhalla) 완료 시 호출(§3.3 "증분 갱신"). 한 트랜잭션 — 도중에 실패하면 전부 롤백된다.
     * 반환값은 "이번 트랙에서 새로 채택된 edge"만 집계한 [MatchRecordSummary] — 러닝 결과 화면용. */
    fun recordMatchResult(
        trackId: String,
        edges: List<MatchedEdge>,
        confidenceScore: Double,
        unmatchedPointCount: Int,
        boundaries: List<DongBoundary>,
        nowEpochMs: Long,
    ): MatchRecordSummary {
        var newRoadMeters = 0.0
        val touchedDongAdmCodes = mutableSetOf<String>()
        database.transaction {
            edges.forEachIndexed { index, edge ->
                val seq = index.toLong()
                database.trackEdgeQueries.insertEdge(
                    track_id = trackId,
                    seq = seq,
                    way_id = edge.wayId,
                    coordinates = encodeCoordinates(edge.coordinates),
                    length_meters = GeoMath.totalDistanceMeters(edge.coordinates),
                )
                if (adoptEdge(edge.wayId, trackId, seq)) {
                    newRoadMeters += GeoMath.totalDistanceMeters(edge.coordinates)
                    splitLengthByDong(boundaries, edge.coordinates).forEach { (admCd2, length) ->
                        database.dongCoverageQueries.insertEdgeDongLength(trackId, seq, admCd2, length)
                        touchedDongAdmCodes += admCd2
                    }
                }
            }
            database.trackQueries.updateMatchState(
                match_state = MATCH_STATE_MATCHED,
                confidence_score = confidenceScore,
                unmatched_point_count = unmatchedPointCount.toLong(),
                updated_at_epoch_ms = nowEpochMs,
                id = trackId,
            )
        }
        return MatchRecordSummary(newRoadMeters, touchedDongAdmCodes)
    }

    /** `way_id`가 없으면 항상 채택(중복 제거 대상이 아님). 있으면 전역에서 처음 나온 edge만 채택
     * (`INSERT OR IGNORE` + `changes()`로 실제 insert됐는지 확인 — 같은 트랜잭션 안에서 바로 이어
     * 호출해야 한다). */
    private fun adoptEdge(wayId: Long?, trackId: String, seq: Long): Boolean {
        if (wayId == null) return true
        database.dongCoverageQueries.insertVisitedWayIfAbsent(wayId, trackId, seq)
        return database.dongCoverageQueries.changes().executeAsOne() > 0
    }

    private fun splitLengthByDong(boundaries: List<DongBoundary>, coordinates: List<LatLon>): Map<String, Double> {
        val lengths = mutableMapOf<String, Double>()
        for (i in 0 until coordinates.size - 1) {
            val a = coordinates[i]
            val b = coordinates[i + 1]
            val midpoint = LatLon(lat = (a.lat + b.lat) / 2, lon = (a.lon + b.lon) / 2)
            val admCd2 = DongCoverageCalculator.findDong(boundaries, midpoint) ?: continue
            lengths[admCd2] = (lengths[admCd2] ?: 0.0) + GeoMath.distanceMeters(a, b)
        }
        return lengths
    }

    /** 동별 완주 길이(분자) — [DongCoverageCalculator]를 매번 다시 돌리지 않고 캐시 테이블에서 바로
     * 읽는다. */
    fun walkedLengthByDong(): Map<String, Double> =
        database.dongCoverageQueries.walkedLengthByDong().executeAsList()
            .associate { it.adm_cd2 to (it.walked_meters ?: 0.0) }

    /** 가본 길 렌더링(§3.2): 트랙별로 seq 순서대로 합쳐 트랙당 Feature 1개가 되게 한다 — 안 그러면
     * glow 레이어가 wayId 경계마다 round cap이 겹쳐서 꺾이는 지점에 빛이 번진다. */
    fun visitedRoadEdges(): List<MatchedEdge> =
        database.trackEdgeQueries.selectAllEdgesForRendering().executeAsList()
            .groupBy { it.track_id }
            .values
            .map { rows -> MatchedEdge(wayId = null, coordinates = rows.flatMap { decodeCoordinates(it.coordinates) }) }

    /** 트랙 삭제(§5): DB row 삭제(→ edge/note CASCADE, course는 SET NULL) → 완주율 전체 재생성 →
     * GPX 파일 삭제. 중간에 실패해도 고아 GPX만 남고 기능엔 영향 없다. */
    fun deleteTrack(id: String, boundaries: List<DongBoundary>) {
        val track = database.trackQueries.selectById(id).executeAsOneOrNull() ?: return
        database.trackQueries.deleteTrack(id)
        regenerateCoverage(boundaries)
        fileStore.delete(track.gpx_filtered_path)
        fileStore.delete(track.gpx_raw_path)
    }

    /** §3.3 "트랙 삭제/재생성": wayId "처음 집계" 결과가 트랙 시작 시각 순서에 의존하기 때문에, 캐시를
     * 전부 지우고 남은 edge를 시작 시각 순으로 다시 돌리는 것 말고는 안전하게 갱신할 방법이 없다. 동
     * 경계 데이터(`korea_dong.geojson`)가 바뀔 때도 같은 함수를 쓴다. */
    fun regenerateCoverage(boundaries: List<DongBoundary>) {
        database.transaction {
            database.dongCoverageQueries.clearEdgeDongLength()
            database.dongCoverageQueries.clearVisitedWay()
            database.trackEdgeQueries.selectAllEdgesForCoverageRebuild().executeAsList().forEach { row ->
                if (adoptEdge(row.way_id, row.track_id, row.seq)) {
                    splitLengthByDong(boundaries, decodeCoordinates(row.coordinates)).forEach { (admCd2, length) ->
                        database.dongCoverageQueries.insertEdgeDongLength(row.track_id, row.seq, admCd2, length)
                    }
                }
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
