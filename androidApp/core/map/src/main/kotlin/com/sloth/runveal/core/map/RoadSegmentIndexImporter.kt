package com.sloth.runveal.core.map

import android.content.Context
import com.sloth.runveal.db.RunvealDatabase
import com.sloth.runveal.dong.DongCoverageCalculator
import com.sloth.runveal.dong.RoadSegmentParser
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.meta.MetaHolder
import java.io.File
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val META_WAIT_TIMEOUT_MS = 5_000L

/**
 * 서버 지도 파일 `segments-{구코드}.geojson`(`docs/api/API.md` §3.1.2)을 로컬 `road_segment`
 * 인덱스로 들여온다. `adm_cd2`는 **서버 `index.geojson`의 동 경계**로 계산한다 — 완주율의 분자(이
 * 인덱스)와 분모(`index.json`의 `dongLengthM`)가 같은 코드 체계를 써야 하기 때문이다. 예전엔 번들
 * `korea_dong.geojson`으로 판정했는데, 그건 서버 연동 전 테스트용 스냅샷이라 코드 체계가 달랐고
 * (광주·전남 통합 `12` vs 분리 `29`/`46` 등 약 13%) 그 지역 완주율이 분모와 안 맞아 0%로 떨어졌다.
 * 구코드로
 * 계층 정보에서 그 구에 속한 동만 먼저 추려서 point-in-polygon 대상을 줄인다(전국 3,559개 동을
 * 매 구간마다 전부 검사하면 너무 느리다).
 *
 * **전국이 아니라 "이번에 달린 구"만 들여온다.** 전국은 파일 255개 896MB에 구간 약 400만 개라,
 * 러닝이 끝날 때마다 그걸 받아 파싱하느라 결과 화면이 무한 로딩에 빠졌다(한 트랜잭션에 묶여 있어서
 * 끝내 완료되지도 못했고, 그래서 다음 러닝에서 또 처음부터 다시 시도했다). 구 하나는 2~3MB에
 * 1만 구간 남짓이라 몇 초면 끝난다.
 */
class RoadSegmentIndexImporter(
    private val context: Context,
    private val database: RunvealDatabase,
) {
    /**
     * [sigunguCodes](`/match` 응답의 구 코드 5자리)에 해당하는 구 중 아직 안 들여온 것만 받아서
     * 인덱스에 넣는다. 전부 이미 있으면 즉시 `true`. 실패(오프라인, 서버에 그 구 파일이 없음 등)하면
     * `false` — 호출부는 그 러닝을 집계에서 빼고 넘어간다.
     */
    suspend fun importIfNeeded(sigunguCodes: List<String>): Boolean {
        val missing = sigunguCodes.distinct().filter { guCode ->
            database.roadSegmentIndexQueries.countBySigungu("$guCode%").executeAsOne() == 0L
        }
        if (missing.isEmpty()) return true
        val meta = withTimeoutOrNull(META_WAIT_TIMEOUT_MS) { MetaHolder.state.filterNotNull().first() } ?: return false
        return try {
            val store = ServerMapFileStore(context)
            val repository = DongAssetRepository(context)
            // 서버 동 경계를 못 받으면(오프라인 등) 인덱스를 만들지 않는다 — 번들로 대신 만들면
            // 코드 체계가 섞여서 완주율이 조용히 틀어진다. 다음 러닝에서 다시 시도한다.
            val server = repository.syncFromServer() ?: return false
            val boundariesByAdmCd2 = repository.loadBoundaries(server.boundaryGeoJson).associateBy { it.admCd2 }
            val dongCodesBySigungu = repository.loadRegionHierarchy().sido
                .flatMap { it.sigungu }
                .associate { it.code to it.dongs.map { dong -> dong.code } }

            missing.forEach { guCode ->
                val fileName = "segments-$guCode.geojson"
                val file = File(store.ensureDownloaded(meta.mapVersions.current, listOf(fileName)), fileName)
                if (!file.exists()) return@forEach
                val candidateBoundaries = dongCodesBySigungu[guCode]
                    ?.mapNotNull { boundariesByAdmCd2[it] }
                    ?.takeIf { it.isNotEmpty() }
                    ?: boundariesByAdmCd2.values.toList()
                // 구 단위로 트랜잭션을 끊는다 — 여러 구를 한 트랜잭션에 묶으면 중간에 실패했을 때
                // 먼저 끝난 구까지 통째로 롤백돼서, 다음 러닝에 또 처음부터 받게 된다.
                database.transaction {
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

    private fun List<LatLon>.midpointOrNull(): LatLon? = if (isEmpty()) null else this[size / 2]
}
