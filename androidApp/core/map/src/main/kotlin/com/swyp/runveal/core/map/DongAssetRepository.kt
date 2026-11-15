package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.core.map.internal.maplibre.DongGeoJsonBuilder
import com.swyp.runveal.core.map.internal.maplibre.SigunguGeoJsonBuilder
import com.swyp.runveal.core.map.internal.maplibre.VisitedRoadGeoJsonBuilder
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.dong.MatchedEdgeParser
import com.swyp.runveal.dong.region.RegionHierarchy
import com.swyp.runveal.dong.region.RegionHierarchyParser
import com.swyp.runveal.network.maps.IndexResponse
import com.swyp.runveal.network.meta.MetaHolder
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/** `"code":` / `"code" :` / `"code": ` 어느 형태든 잡는다(서버 JSON 포맷이 바뀌어도 깨지지 않게). */
private val DONG_CODE_PROPERTY_REGEX = Regex("\"code\"\\s*:")

private const val SIGUNGU_BOUNDARY_ASSET_PATH = "dong/korea_sigungu.geojson"
private const val SIDO_BOUNDARY_ASSET_PATH = "dong/korea_sido.geojson"
private const val REGION_HIERARCHY_ASSET_PATH = "dong/korea_region_hierarchy.json"
/** 실제로 기록된 트랙(`docs/map/local-db-schema-design.md` §8)의 `<trackId>.matched.geojson`을
 * 모아 둔 디렉터리 — 개인 위치 기록이라 `.gitignore`로 제외했다(`sample-data/tracks/`와 같은 원본). */
private const val VISITED_SAMPLE_TRACKS_ASSET_DIR = "tracks"

/** [DongAssetRepository.syncFromServer]가 스플래시에서 이미 쏴 둔 `/api/v1/meta` 결과([MetaHolder])를
 * 기다리는 최대 시간 — 서버가 꺼져 있거나 느려도 홈 지도는 번들 에셋으로 계속 떠야 한다(§5.7). */
private const val META_WAIT_TIMEOUT_MS = 5000L

/**
 * 동/시군구/시도 경계를 읽어 [DongOverlayGeoJson]/[SigunguOverlayGeoJson]/[SidoOverlayGeoJson]을 만든다.
 *
 * **동 경계와 완주율 분모는 서버만 쓴다**([syncFromServer] — `index.geojson`/`index.json`). 예전엔 못
 * 받았을 때 번들 `korea_dong.geojson`으로 떨어졌는데, 그건 서버 연동 전 테스트용 스냅샷이라 코드
 * 체계가 달라서(광주·전남 통합 등 약 13%) 서버 분모와 안 맞았다 — 섞어 쓰느니 그 경우엔 동을 안
 * 그리는 쪽이 낫다.
 *
 * 시군구/시도 경계와 계층(`korea_sigungu.geojson` 등)은 서버가 안 주는 자료라 번들로 둔다. 다만
 * `tools/gen_region_assets.py`로 **서버 `index.geojson`에서 생성**해 코드 체계를 맞춰 뒀다.
 */
class DongAssetRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cachedRegionHierarchy: RegionHierarchy? = null

    /** [rawGeoJson]은 [syncFromServer]로 받은 서버 `index.geojson`. */
    fun loadBoundaries(rawGeoJson: String): List<DongBoundary> =
        DongBoundaryParser.parse(rawGeoJson)

    /** 시도 16 → 시군구 256 → 행정동 3,559 계층(`docs/map/refactoring/REGION_HIERARCHY.md`). 한 번
     * 파싱한 결과를 재사용한다 — 다른 동/시군구/시도 경계 에셋(수백 KB~4MB)도 전부 호출부(`remember{}`)
     * 안에서 동기로 읽는 것과 같은 방식이라, 이 160KB 파일만 비동기로 떼어 두지 않는다. */
    fun loadRegionHierarchy(): RegionHierarchy {
        cachedRegionHierarchy?.let { return it }
        return RegionHierarchyParser.parse(readAsset(REGION_HIERARCHY_ASSET_PATH)).also { cachedRegionHierarchy = it }
    }

    /** 구멍(hole)을 보존한 폴리곤 — 동 빛 이미지 생성([com.swyp.runveal.dong.glow.DongGlowRenderer])에만 쓴다. */
    fun loadSigunguBoundaries(): List<DongBoundary> = DongBoundaryParser.parse(readAsset(SIGUNGU_BOUNDARY_ASSET_PATH))

    fun loadSidoBoundaries(): List<DongBoundary> = DongBoundaryParser.parse(readAsset(SIDO_BOUNDARY_ASSET_PATH))

    /** 스플래시가 이미 받아 둔 `/api/v1/meta`([MetaHolder])의 지도 버전으로 서버 지도 파일을 내려받고,
     * `index.geojson`/`index.json`을 홈 지도가 바로 쓸 수 있게 뽑아 온다. 실패하거나(오프라인·서버 꺼짐)
     * [MetaHolder]가 아직 안 채워졌으면 `null` — 호출부는 번들 에셋을 그대로 쓰면 된다. */
    suspend fun syncFromServer(): ServerDongMapData? {
        val meta = withTimeoutOrNull(META_WAIT_TIMEOUT_MS) { MetaHolder.state.filterNotNull().first() } ?: return null
        return try {
            val dir = ServerMapFileStore(context).ensureDownloaded(meta.mapVersions.current, listOf("index.geojson", "index.json"))
            val boundaryGeoJson = File(dir, "index.geojson").readText().normalizeDongCodeKey()
            val totalLengthsByDong = json.decodeFromString<IndexResponse>(File(dir, "index.json").readText()).dongLengthM
            ServerDongMapData(
                boundaryGeoJson = boundaryGeoJson,
                totalLengthsByDong = totalLengthsByDong,
                mapVersion = meta.mapVersions.current,
            )
        } catch (_: Exception) {
            null
        }
    }

    /** 서버 `index.geojson`은 행정동 코드를 `code` 프로퍼티로 주는데(`docs/api/API.md` §3.1.2의
     * 플레이스홀더 경고 — 전국 데이터로 바뀐 뒤에도 키 이름은 그대로임), [DongBoundaryParser]를 비롯해
     * 이 raw GeoJSON을 직접 읽는 모든 곳(`DongGeoJsonBuilder`/`SigunguGeoJsonBuilder`/
     * `DongGlowImageLayer`)이 번들 에셋과 같은 `adm_cd2` 키를 기대한다 — 값은 서로 같아서(둘 다 10자리
     * 행정동 코드) 키 이름만 바꿔 맞춘다. 파서들을 전부 고치는 대신 서버 데이터가 들어오는 이 지점
     * 한 곳에서만 정규화한다.
     *
     * 콜론 뒤 공백은 정규식으로 흡수한다 — 2026-11-15 배포에서 서버가 `{"code":"…"}`에서
     * `{"code": "…"}`(공백 포함)로 포맷이 바뀌자 치환이 한 건도 안 먹어서, 모든 동에 코드가 없다고
     * 판단해 홈 지도의 동 테두리·라벨·빛이 통째로 사라졌다. */
    private fun String.normalizeDongCodeKey(): String = replace(DONG_CODE_PROPERTY_REGEX, "\"adm_cd2\":")

    /** [VISITED_SAMPLE_TRACKS_ASSET_DIR]에 있는 실제 기록 트랙을 전부 읽어 합친다 — 가본 길 렌더링을
     * 시연하는 용도. 디렉터리 자체가 없으면(에셋을 안 넣은 빌드) 빈
     * 목록을 돌려준다. 트랙(파일) 하나는 [mergeIntoSingleLine]으로 합쳐 한 Feature로 그린다 — 조각마다
     * 따로 그리면 glow 레이어(반투명 + blur)가 선 끝 round cap끼리 겹쳐서 꺾이는 지점마다 빛이 번져
     * 보인다. */
    fun loadSampleVisitedEdges(): List<MatchedEdge> {
        val fileNames = context.assets.list(VISITED_SAMPLE_TRACKS_ASSET_DIR) ?: return emptyList()
        return fileNames
            .filter { it.endsWith(".matched.geojson") }
            .map { name -> mergeIntoSingleLine(MatchedEdgeParser.parse(readAsset("$VISITED_SAMPLE_TRACKS_ASSET_DIR/$name"))) }
    }

    private fun mergeIntoSingleLine(edges: List<MatchedEdge>): MatchedEdge =
        MatchedEdge(coordinates = edges.flatMap { it.coordinates })

    /** 가본 길([VisitedRoadLayer]) GeoJSON — 동/시군구/시도 경계 에셋과 무관하게 [edges]만으로 만들어진다. */
    fun buildVisitedRoadGeoJson(edges: List<MatchedEdge>): String = VisitedRoadGeoJsonBuilder.build(edges)

    /** [dongPercents]는 내 기록 기준 동별 탐험률(%) — `TrackLocalStore`의 가본 길 길이와 그 동의 전체
     * 도로 길이로 [com.swyp.runveal.dong.DongExploreRate]가 계산한다. [rawBoundaryGeoJson]은
     * [syncFromServer]로 받은 서버 `index.geojson`. */
    fun buildOverlay(
        dongPercents: Map<String, Int>,
        rawBoundaryGeoJson: String,
    ): DongOverlayResult {
        val boundaries = loadBoundaries(rawBoundaryGeoJson)
        return buildOverlayFrom(boundaries, rawBoundaryGeoJson, dongPercents)
    }

    /** [buildOverlay]와 같은 렌더링 파이프라인을, 번들 에셋이 아니라 외부에서 받아 온 동 경계 geoJSON과
     * 완주율로 돌린다 — 서버 지도 파일([ServerMapFileStore], `docs/api/API.md` §3.1.2)로 받은
     * `index.geojson`을 적용해 볼 때 쓴다. [rawBoundaryGeoJson]의 각 feature는 [boundaries]와 같은
     * `adm_cd2` 프로퍼티를 가지고 있어야 한다. */
    fun buildOverlayFrom(boundaries: List<DongBoundary>, rawBoundaryGeoJson: String, percents: Map<String, Int>): DongOverlayResult {
        val result = DongGeoJsonBuilder.build(boundaries, rawBoundaryGeoJson, percents)
        return DongOverlayResult(
            overlay = DongOverlayGeoJson(
                fillGeoJson = result.fillGeoJson,
                lineGeoJson = result.lineGeoJson,
                labelGeoJson = result.labelGeoJson,
            ),
            polygonPartsByAdmCd2 = result.polygonPartsByAdmCd2,
        )
    }

    /** 줌아웃 시 보여줄 시군구 테두리/라벨 — 테두리 단계는 소속 동들의 완주율 평균([aggregateBySigungu]). */
    fun buildSigunguOverlay(dongPercents: Map<String, Int>): SigunguOverlayGeoJson {
        val boundaries = loadSigunguBoundaries()
        val sigunguPercents = aggregateBySigungu(dongPercents, loadRegionHierarchy())
        val result = SigunguGeoJsonBuilder.build(boundaries, readAsset(SIGUNGU_BOUNDARY_ASSET_PATH), sigunguPercents)
        return SigunguOverlayGeoJson(
            lineGeoJson = result.lineGeoJson,
            labelGeoJson = result.labelGeoJson,
        )
    }

    /** 더 줌아웃하면 보여줄 시도 테두리/라벨 — 테두리 단계는 소속 시군구 평균의 평균([aggregateBySido]). */
    fun buildSidoOverlay(dongPercents: Map<String, Int>): SidoOverlayGeoJson {
        val boundaries = loadSidoBoundaries()
        val hierarchy = loadRegionHierarchy()
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
