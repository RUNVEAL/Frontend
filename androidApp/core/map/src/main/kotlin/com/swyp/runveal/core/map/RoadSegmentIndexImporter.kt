package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.DongCoverageCalculator
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.network.meta.MetaHolder
import java.io.File
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val META_WAIT_TIMEOUT_MS = 5_000L

/**
 * 서버 지도 파일 `segments-{구코드}.geojson`(`docs/api/API.md` §3.1.2) 전체를 로컬 `road_segment`
 * 인덱스로 한 번만 들여온다. `adm_cd2`는 서버 `index.geojson`이 아니라 번들 동 경계([DongAssetRepository])
 * 로 직접 계산한다 — 그래야 서버 동 데이터 연동 여부와 무관하게 항상 동작한다. 구코드(파일명)로
 * [RegionHierarchy]에서 그 구에 속한 동만 먼저 추려서 point-in-polygon 대상을 줄인다(전국 3,559개
 * 동을 매 구간마다 전부 검사하면 너무 느리다).
 *
 * 이미 들여왔으면([road_segment]가 비어있지 않으면) 다시 하지 않는다 — [importIfNeeded]는 아무 때나
 * 다시 불러도 안전하다.
 */
class RoadSegmentIndexImporter(
    private val context: Context,
    private val database: RunvealDatabase,
) {
    /** 이미 들여왔으면 즉시 `true`. 아니면 내려받고 들여온 뒤 `true`, 실패(오프라인 등)하면 `false`. */
    suspend fun importIfNeeded(): Boolean {
        if (database.roadSegmentIndexQueries.countAll().executeAsOne() > 0) return true
        val meta = withTimeoutOrNull(META_WAIT_TIMEOUT_MS) { MetaHolder.state.filterNotNull().first() } ?: return false
        return try {
            val dir = ServerMapFileStore(context).ensureDownloaded(meta.mapVersions.current)
            val repository = DongAssetRepository(context)
            val boundariesByAdmCd2 = repository.loadBoundaries().associateBy { it.admCd2 }
            val dongCodesBySigungu = repository.loadRegionHierarchy().sido
                .flatMap { it.sigungu }
                .associate { it.code to it.dongs.map { dong -> dong.code } }

            database.transaction {
                segmentFiles(dir).forEach { file ->
                    val candidateBoundaries = dongCodesBySigungu[guCodeOf(file)]
                        ?.mapNotNull { boundariesByAdmCd2[it] }
                        ?.takeIf { it.isNotEmpty() }
                        ?: boundariesByAdmCd2.values.toList()
                    RoadSegmentParser.parse(file.readText()).forEach { segment ->
                        val allPoints = segment.lines.flatten()
                        if (allPoints.isEmpty()) return@forEach
                        val midpoint = segment.lines.firstOrNull()?.midpointOrNull() ?: return@forEach
                        val admCd2 = DongCoverageCalculator.findDong(candidateBoundaries, midpoint) ?: return@forEach
                        database.roadSegmentIndexQueries.insertSegment(
                            segment.segmentId,
                            admCd2,
                            segment.lengthM.toDouble(),
                            RoadSegmentParser.encodeLines(segment.lines),
                            allPoints.minOf { it.lat },
                            allPoints.maxOf { it.lat },
                            allPoints.minOf { it.lon },
                            allPoints.maxOf { it.lon },
                        )
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun segmentFiles(dir: File): List<File> =
        dir.listFiles { _, name -> name.startsWith("segments-") && name.endsWith(".geojson") }?.toList() ?: emptyList()

    private fun guCodeOf(file: File): String = file.name.removePrefix("segments-").removeSuffix(".geojson")

    private fun List<LatLon>.midpointOrNull(): LatLon? = if (isEmpty()) null else this[size / 2]
}
