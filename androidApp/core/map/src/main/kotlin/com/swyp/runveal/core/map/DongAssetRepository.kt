package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.core.map.internal.maplibre.DongGeoJsonBuilder
import com.swyp.runveal.core.map.internal.maplibre.SigunguGeoJsonBuilder
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.DongCoverageCalculator
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.dong.region.RegionHierarchy
import com.swyp.runveal.dong.region.RegionHierarchyParser
import java.io.FileNotFoundException
import kotlinx.serialization.json.Json

private const val DONG_BOUNDARY_ASSET_PATH = "dong/korea_dong.geojson"
private const val DONG_LENGTHS_ASSET_PATH = "dong/asan_dong_lengths.json"
private const val SIGUNGU_BOUNDARY_ASSET_PATH = "dong/korea_sigungu.geojson"
private const val SIDO_BOUNDARY_ASSET_PATH = "dong/korea_sido.geojson"
private const val REGION_HIERARCHY_ASSET_PATH = "dong/korea_region_hierarchy.json"

/**
 * 전국 동/시군구/시도 경계 에셋을 읽어 [DongOverlayGeoJson]/[SigunguOverlayGeoJson]/[SidoOverlayGeoJson]을
 * 만든다. [DONG_LENGTHS_ASSET_PATH]는 아산시 전용 샘플이라 이 경계 파일과 더 이상 맞지 않는다 —
 * 완주율(%) 계산을 서버로 옮기기로 하면서 [loadTotalLengthsByDong]/[loadDongPercents]의 분모 로직
 * 자체를 다시 설계해야 해서 일단 그대로 둔다.
 */
class DongAssetRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cachedRegionHierarchy: RegionHierarchy? = null

    fun loadBoundaries(): List<DongBoundary> = DongBoundaryParser.parse(readAsset(DONG_BOUNDARY_ASSET_PATH))

    /** 시도 16 → 시군구 256 → 행정동 3,559 계층(`docs/map/refactoring/REGION_HIERARCHY.md`). 한 번
     * 파싱한 결과를 재사용한다 — 다른 동/시군구/시도 경계 에셋(수백 KB~4MB)도 전부 호출부(`remember{}`)
     * 안에서 동기로 읽는 것과 같은 방식이라, 이 160KB 파일만 비동기로 떼어 두지 않는다. */
    fun loadRegionHierarchy(): RegionHierarchy {
        cachedRegionHierarchy?.let { return it }
        return RegionHierarchyParser.parse(readAsset(REGION_HIERARCHY_ASSET_PATH)).also { cachedRegionHierarchy = it }
    }

    /** 구멍(hole)을 보존한 폴리곤 — 동 빛 이미지 생성([com.swyp.runveal.dong.glow.DongGlowRenderer])에만 쓴다. */
    fun loadPolygonParts(): Map<String, List<DongPolygonPart>> =
        DongBoundaryParser.parsePolygonParts(readAsset(DONG_BOUNDARY_ASSET_PATH))

    fun loadSigunguBoundaries(): List<DongBoundary> = DongBoundaryParser.parse(readAsset(SIGUNGU_BOUNDARY_ASSET_PATH))

    fun loadSidoBoundaries(): List<DongBoundary> = DongBoundaryParser.parse(readAsset(SIDO_BOUNDARY_ASSET_PATH))

    fun loadTotalLengthsByDong(): Map<String, Double> =
        try {
            json.decodeFromString(readAsset(DONG_LENGTHS_ASSET_PATH))
        } catch (_: FileNotFoundException) {
            // [korea_dong.geojson]로 바뀌면서 아산시 전용이던 이 에셋은 더 이상 없다 — 완주율(%)은
            // 서버에서 받아오는 걸로 바뀔 예정이라 분모가 없으면 전부 0%로 둔다.
            emptyMap()
        }

    /** 동별 완주율(%). [buildOverlay]뿐 아니라 [buildSigunguOverlay]/[buildSidoOverlay]도 이 값을
     * 집계해서 쓴다 — 시군구/시도 테두리 단계가 소속 동들의 완주율 평균이기 때문. */
    fun loadDongPercents(edges: List<MatchedEdge>, boundaries: List<DongBoundary>): Map<String, Int> {
        val totalLengths = loadTotalLengthsByDong()
        val walkedLengths = DongCoverageCalculator.walkedLengthByDong(boundaries, edges)
        return totalLengths.mapValues { (admCd2, total) ->
            if (total <= 0.0) 0 else (((walkedLengths[admCd2] ?: 0.0) / total) * 100).toInt().coerceIn(0, 100)
        }
    }

    fun buildOverlay(edges: List<MatchedEdge>): DongOverlayGeoJson {
        val boundaries = loadBoundaries()
        val percents = loadDongPercents(edges, boundaries)
        val result = DongGeoJsonBuilder.build(boundaries, readAsset(DONG_BOUNDARY_ASSET_PATH), percents)
        return DongOverlayGeoJson(
            fillGeoJson = result.fillGeoJson,
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    /** 줌아웃 시 보여줄 시군구 테두리/라벨 — 테두리 단계는 소속 동들의 완주율 평균([aggregateBySigungu]). */
    fun buildSigunguOverlay(edges: List<MatchedEdge>): SigunguOverlayGeoJson {
        val boundaries = loadSigunguBoundaries()
        val dongPercents = loadDongPercents(edges, loadBoundaries())
        val sigunguPercents = aggregateBySigungu(dongPercents, loadRegionHierarchy())
        val result = SigunguGeoJsonBuilder.build(boundaries, readAsset(SIGUNGU_BOUNDARY_ASSET_PATH), sigunguPercents)
        return SigunguOverlayGeoJson(
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    /** 더 줌아웃하면 보여줄 시도 테두리/라벨 — 테두리 단계는 소속 시군구 평균의 평균([aggregateBySido]). */
    fun buildSidoOverlay(edges: List<MatchedEdge>): SidoOverlayGeoJson {
        val boundaries = loadSidoBoundaries()
        val hierarchy = loadRegionHierarchy()
        val dongPercents = loadDongPercents(edges, loadBoundaries())
        val sigunguPercents = aggregateBySigungu(dongPercents, hierarchy)
        val sidoPercents = aggregateBySido(sigunguPercents, hierarchy)
        val result = SigunguGeoJsonBuilder.build(boundaries, readAsset(SIDO_BOUNDARY_ASSET_PATH), sidoPercents)
        return SidoOverlayGeoJson(
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    /** 시군구 완주율 = 소속 행정동 완주율의 평균(동이 없으면 0). 올림(ceiling)이라 소속 동 중 하나라도
     * 완주율이 있으면 평균이 0으로 뭉개지지 않는다 — 시군구 밑에 동이 수십 개면 보통의 내림(또는 절삭)
     * 평균은 거의 항상 0단계로 사라진다. */
    private fun aggregateBySigungu(dongPercents: Map<String, Int>, hierarchy: RegionHierarchy): Map<String, Int> =
        hierarchy.sido.flatMap { it.sigungu }.associate { sigungu ->
            val dongCodes = sigungu.dongs.map { it.code }
            val sum = dongCodes.sumOf { dongPercents[it] ?: 0 }
            sigungu.code to ceilDivide(sum, dongCodes.size)
        }

    /** 시도 완주율 = 소속 시군구 완주율의 평균(시군구가 없으면 0). [aggregateBySigungu]와 같은 이유로 올림. */
    private fun aggregateBySido(sigunguPercents: Map<String, Int>, hierarchy: RegionHierarchy): Map<String, Int> =
        hierarchy.sido.associate { sido ->
            val sigunguCodes = sido.sigungu.map { it.code }
            val sum = sigunguCodes.sumOf { sigunguPercents[it] ?: 0 }
            sido.code to ceilDivide(sum, sigunguCodes.size)
        }

    private fun ceilDivide(sum: Int, count: Int): Int = if (count <= 0) 0 else (sum + count - 1) / count

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
