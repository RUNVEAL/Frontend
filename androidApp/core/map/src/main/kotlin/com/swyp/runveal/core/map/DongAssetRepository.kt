package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.core.map.internal.maplibre.DongGeoJsonBuilder
import com.swyp.runveal.core.map.internal.maplibre.SigunguGeoJsonBuilder
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.DongCoverageCalculator
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.MatchedEdge
import java.io.FileNotFoundException
import kotlinx.serialization.json.Json

private const val DONG_BOUNDARY_ASSET_PATH = "dong/korea_dong.geojson"
private const val DONG_LENGTHS_ASSET_PATH = "dong/asan_dong_lengths.json"
private const val SIGUNGU_BOUNDARY_ASSET_PATH = "dong/korea_sigungu.geojson"
private const val SIDO_BOUNDARY_ASSET_PATH = "dong/korea_sido.geojson"

/** 테스트용: 완주율 서버 연동 전까지 동 빛 이미지를 눈으로 확인하기 위한 아산시 온양1~6동 샘플(행정동
 * 코드, `docs/map/refactoring/HJD_MIGRATION.md` Step 3). 연동되면 지워질 값이라 adm_cd2만 두고
 * 동 이름은 주석으로 남긴다. */
private val TEST_ASAN_DONG_PERCENTS = mapOf(
    "4420057000" to 3, // 온양1동 - level 1
    "4420058000" to 8, // 온양2동 - level 2
    "4420059000" to 15, // 온양3동 - level 3
    "4420060000" to 30, // 온양4동 - level 4
    "4420061000" to 55, // 온양5동 - level 5
    "4420062000" to 85, // 온양6동 - level 6
)

/**
 * 전국 동/시군구 경계 에셋을 읽어 [DongOverlayGeoJson]/[SigunguOverlayGeoJson]을 만든다.
 * [DONG_LENGTHS_ASSET_PATH]는 아산시 전용 샘플이라 이 경계 파일과 더 이상 맞지 않는다 — 완주율(%)
 * 계산을 서버로 옮기기로 하면서 [loadTotalLengthsByDong]/[buildOverlay]의 분모 로직 자체를 다시
 * 설계해야 해서 일단 그대로 둔다.
 */
class DongAssetRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadBoundaries(): List<DongBoundary> = DongBoundaryParser.parse(readAsset(DONG_BOUNDARY_ASSET_PATH))

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

    fun buildOverlay(edges: List<MatchedEdge>): DongOverlayGeoJson {
        val boundaries = loadBoundaries()
        val totalLengths = loadTotalLengthsByDong()
        val walkedLengths = DongCoverageCalculator.walkedLengthByDong(boundaries, edges)
        val percents = totalLengths.mapValues { (admCd2, total) ->
            if (total <= 0.0) 0 else (((walkedLengths[admCd2] ?: 0.0) / total) * 100).toInt().coerceIn(0, 100)
        } + TEST_ASAN_DONG_PERCENTS
        val result = DongGeoJsonBuilder.build(boundaries, readAsset(DONG_BOUNDARY_ASSET_PATH), percents)
        return DongOverlayGeoJson(
            fillGeoJson = result.fillGeoJson,
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    /** 줌아웃 시 보여줄 시군구 테두리/라벨 — 완주율 개념이 없어 [buildOverlay]와 달리 edges/percent가 없다. */
    fun buildSigunguOverlay(): SigunguOverlayGeoJson {
        val boundaries = loadSigunguBoundaries()
        val result = SigunguGeoJsonBuilder.build(boundaries, readAsset(SIGUNGU_BOUNDARY_ASSET_PATH))
        return SigunguOverlayGeoJson(
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    /** 더 줌아웃하면 보여줄 시도 테두리/라벨 — [buildSigunguOverlay]와 같은 이유로 percent가 없다. */
    fun buildSidoOverlay(): SidoOverlayGeoJson {
        val boundaries = loadSidoBoundaries()
        val result = SigunguGeoJsonBuilder.build(boundaries, readAsset(SIDO_BOUNDARY_ASSET_PATH))
        return SidoOverlayGeoJson(
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
