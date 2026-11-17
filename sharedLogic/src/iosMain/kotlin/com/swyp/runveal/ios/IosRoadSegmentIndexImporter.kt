package com.swyp.runveal.ios

import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.DongCoverageCalculator
import com.swyp.runveal.dong.RoadSegmentParser
import com.swyp.runveal.dong.region.RegionHierarchyParser
import com.swyp.runveal.map.LatLon
/**
 * 서버 지도 파일 `segments-{구코드}.geojson`(`docs/api/API.md` §3.1.2)을 로컬 `road_segment` 인덱스로
 * 들여온다 — Android `core:map`의 `RoadSegmentIndexImporter`와 같은 로직.
 *
 * `adm_cd2`는 구간 중점을 **서버 동 경계**에 point-in-polygon해서 직접 계산한다. 번들 경계로 계산하면
 * 서버와 행정동 코드 체계가 달라 완주율이 조용히 틀어지므로, 경계를 못 받으면 인덱스를 만들지 않고
 * 다음 러닝에서 다시 시도한다([IosServerDongData]). 구코드로 계층 정보에서 그 구에 속한 동만 먼저
 * 추려서 point-in-polygon 대상을 줄인다 — 계층 파일은 서버가 주지 않아 번들에 남아 있다.
 *
 * **전국이 아니라 "이번에 달린 구"만 들여온다.** 전국은 파일 255개 896MB에 구간 약 400만 개라,
 * 러닝이 끝날 때마다 받아 파싱하면 결과 화면이 무한 로딩에 빠진다. 구 하나는 2~3MB에 1만 구간
 * 남짓이라 몇 초면 끝난다.
 */
class IosRoadSegmentIndexImporter(
    private val database: RunvealDatabase = RunvealDatabaseProvider.database,
    private val serverMapStore: IosServerMapStore = IosServerMapStore(),
) {

    /**
     * [sigunguCodes](`/match` 응답의 구 코드 5자리)에 해당하는 구 중 아직 안 들여온 것만 받아서
     * 인덱스에 넣는다. 전부 이미 있으면 즉시 `true`. 실패(오프라인, 서버에 그 구 파일이 없음 등)하면
     * `false` — 호출부는 그 러닝을 집계에서 빼고 넘어간다.
     *
     * @param regionHierarchyJson 번들 계층 원본(Swift가 번들에서 읽어 넘긴다).
     */
    suspend fun importIfNeeded(
        sigunguCodes: List<String>,
        regionHierarchyJson: String,
    ): Boolean {
        val missing = sigunguCodes.distinct().filter { guCode ->
            database.roadSegmentIndexQueries.countBySigungu("$guCode%").executeAsOne() == 0L
        }
        if (missing.isEmpty()) return true

        // 동 경계를 못 받으면(오프라인 등) 인덱스를 만들지 않는다 — 번들로 대신 만들면 코드 체계가
        // 섞여서 완주율이 조용히 틀어진다. 다음 러닝에서 다시 시도한다.
        val server = IosServerDongData.fetch() ?: return false

        return try {
            val boundariesByAdmCd2 = DongBoundaryParser.parse(server.boundaryGeoJson).associateBy { it.admCd2 }
            val dongCodesBySigungu = RegionHierarchyParser.parse(regionHierarchyJson).sido
                .flatMap { it.sigungu }
                .associate { sigungu -> sigungu.code to sigungu.dongs.map { it.code } }

            missing.forEach { guCode ->
                val fileName = "segments-$guCode.geojson"
                val files = serverMapStore.ensureDownloaded(server.mapVersion, listOf(fileName))
                val text = files[fileName] ?: return@forEach
                val candidateBoundaries = dongCodesBySigungu[guCode]
                    ?.mapNotNull { boundariesByAdmCd2[it] }
                    ?.takeIf { it.isNotEmpty() }
                    ?: boundariesByAdmCd2.values.toList()

                // 구 단위로 트랜잭션을 끊는다 — 여러 구를 한 트랜잭션에 묶으면 중간에 실패했을 때
                // 먼저 끝난 구까지 통째로 롤백돼서, 다음 러닝에 또 처음부터 받게 된다.
                database.transaction {
                    RoadSegmentParser.parse(text).forEach { segment ->
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
