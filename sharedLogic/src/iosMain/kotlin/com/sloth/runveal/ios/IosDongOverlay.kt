package com.sloth.runveal.ios

import com.sloth.runveal.dong.DongBoundary
import com.sloth.runveal.dong.DongBoundaryParser
import com.sloth.runveal.dong.DongPolygonPart
import com.sloth.runveal.dong.Polylabel
import com.sloth.runveal.dong.region.RegionHierarchy
import com.sloth.runveal.map.LatLon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.abs

/**
 * 동/시군구/시도 오버레이 한 벌 — MapLibre 소스에 그대로 넣는 GeoJSON 문자열들.
 * Android `core:map`의 `DongOverlayGeoJson`/`SigunguOverlayGeoJson`/`SidoOverlayGeoJson`을 합친 모양이다.
 */
data class DongOverlayGeoJsonBundle(
    val dongFillGeoJson: String,
    val dongLineGeoJson: String,
    val dongLabelGeoJson: String,
    val sigunguLineGeoJson: String,
    val sigunguLabelGeoJson: String,
    val sidoLineGeoJson: String,
    val sidoLabelGeoJson: String,
    /**
     * 전국 폴리곤·단계·탐험률 — **Swift로 복사하지 않고** 여기 두고 묻는다([DongOverlayQuery]).
     *
     * 빛 이미지도 테두리 선과 **같은 정점**을 쓰도록 이 안에 같이 담는다 — 경계 GeoJSON을 따로
     * 한 번 더 파싱하면 전국 데이터를 두 번 읽게 되고, 전처리(ring 방향 정규화)가 어긋날 여지도 생긴다.
     */
    val query: DongOverlayQuery,
    /** 이 오버레이를 만든 경계 데이터의 출처 — `"server:<지도 버전>"`(경계를 못 받았으면 `"none"`).
     * 빛 이미지 디스크 캐시 키에 들어가서, 서버가 경계를 고쳐 새 버전으로 배포하면 옛 경계로
     * 만들어 둔 빛 이미지가 자동으로 버려진다(안 그러면 새 테두리와 어긋난 채로 남는다). */
    val boundarySourceKey: String,
) {
    /**
     * 서버에서 동 경계를 못 받아 비어 있는 묶음인지([empty]로 만든 것). 참이면 동 테두리·라벨·빛이
     * 통째로 안 보이고 탐험률이 전부 0%가 되는데, 설명이 없으면 "원래 그런 화면"처럼 보인다 —
     * 홈이 이 값으로 "지도 데이터를 받지 못했어요" 배너를 띄운다(Android `isServerDataMissing`).
     */
    val isServerDataMissing: Boolean get() = boundarySourceKey == NO_BOUNDARY_SOURCE_KEY

    companion object {
        /** [boundarySourceKey]가 이 값이면 동 경계가 없다는 뜻 — [isServerDataMissing] 참고. */
        const val NO_BOUNDARY_SOURCE_KEY: String = "none"

        /**
         * 서버 동 경계를 못 받았을 때 — 동/시군구/시도 레이어를 비워 둔다. 지도는 그대로 뜨고 경계만
         * 안 보인다. 다음에 다시 만들 때 서버가 살아 있으면 정상으로 채워진다.
         *
         * 탐험률도 같이 비운다 — 분자·분모는 로컬에도 있지만, 서버와 코드 체계가 어긋난 채로 숫자만
         * 띄우면 지도는 안 빛나는데 카드만 8%라고 하는 상태가 된다.
         */
        fun empty(): DongOverlayGeoJsonBundle {
            val emptyCollection = IosDongOverlayBuilder.emptyFeatureCollection()
            return DongOverlayGeoJsonBundle(
                dongFillGeoJson = emptyCollection,
                dongLineGeoJson = emptyCollection,
                dongLabelGeoJson = emptyCollection,
                sigunguLineGeoJson = emptyCollection,
                sigunguLabelGeoJson = emptyCollection,
                sidoLineGeoJson = emptyCollection,
                sidoLabelGeoJson = emptyCollection,
                // 경계를 못 받았으면 탐험률도 비운다 — 분자·분모는 로컬에도 있지만, 서버와 코드
                // 체계가 어긋난 채로 숫자만 띄우면 지도는 안 빛나는데 헤더만 8%라고 하게 된다.
                query = DongOverlayQuery.empty(),
                boundarySourceKey = NO_BOUNDARY_SOURCE_KEY,
            )
        }
    }
}

/** [DongOverlayQuery.glowCandidates]가 돌려주는, 빛 이미지를 올릴 동 하나. */
data class DongGlowCandidate(
    val admCd2: String,
    val level: Int,
)

/**
 * 전국 동의 폴리곤·단계·탐험률을 **Kotlin 쪽에 두고** 묻는 대로만 답하는 조회 창구.
 *
 * **왜 Swift로 넘기지 않는가.** 이 자료를 Swift `Dictionary`로 캐스팅하면
 * (`as? [String: [DongPolygonPart]]`) ObjC 브리지가 전국 3천여 동의 딕셔너리·배열을 전부 복사한다.
 * 게다가 복사한 뒤에도 좌표는 Kotlin 객체(`LatLon`)로 남아 `point.lat` 접근마다 ObjC 메시지가 가서,
 * 정점을 훑는 쪽(빛 이미지 후보 선별)이 메인 스레드를 길게 잡았다 — 홈 첫 프레임이 커밋되지 못해
 * 스플래시가 그대로 남아 보이던 원인이다.
 *
 * 화면에 보이는 동은 많아야 수십 개이므로, 전부 넘기지 않고 여기서 골라 그만큼만 답한다.
 */
class DongOverlayQuery internal constructor(
    private val partsByAdmCd2: Map<String, List<DongPolygonPart>>,
    private val levelByAdmCd2: Map<String, Int>,
    private val percents: Map<String, Int>,
    private val walkedLengths: Map<String, Double>,
    private val totalLengths: Map<String, Double>,
) {

    /**
     * 코드 → 경계 박스. 만들 때 **한 번만** 정점을 훑어 두고, 화면 판정은 이 네 숫자만 본다 —
     * 카메라가 멈출 때마다 전국 정점을 다시 훑지 않게 하려는 자리다.
     */
    private val boundsByAdmCd2: Map<String, BoundsBox> =
        partsByAdmCd2.mapNotNull { (code, parts) -> boundsOf(parts)?.let { code to it } }.toMap()

    /** 동 경계를 못 받아 비어 있는가 — 빛 이미지 레이어가 이미 올린 이미지를 내릴지 판단한다. */
    val isEmpty: Boolean get() = partsByAdmCd2.isEmpty() || levelByAdmCd2.isEmpty()

    /**
     * 단계 ≥ 1이고 주어진 화면 범위와 겹치는 동을 **화면 중심에 가까운 순으로** 돌려준다.
     * 범위는 남서(min)–북동(max) 좌표로 받는다.
     */
    fun glowCandidates(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        centerLat: Double,
        centerLon: Double,
    ): List<DongGlowCandidate> {
        val hits = mutableListOf<Pair<DongGlowCandidate, Double>>()
        for ((code, level) in levelByAdmCd2) {
            if (level < 1) continue
            val box = boundsByAdmCd2[code] ?: continue
            if (box.minLon > maxLon || box.maxLon < minLon) continue
            if (box.minLat > maxLat || box.maxLat < minLat) continue
            val deltaLat = centerLat - (box.minLat + box.maxLat) / 2
            val deltaLon = centerLon - (box.minLon + box.maxLon) / 2
            hits += DongGlowCandidate(code, level) to (deltaLat * deltaLat + deltaLon * deltaLon)
        }
        return hits.sortedBy { it.second }.map { it.first }
    }

    /** 빛 이미지를 실제로 그릴 때만 그 동의 폴리곤을 꺼낸다 — 화면에 걸린 동만 부른다. */
    fun polygonParts(admCd2: String): List<DongPolygonPart> = partsByAdmCd2[admCd2] ?: emptyList()

    /** 동 탐험률(%). 줌인 헤더의 "내 동네 N%" — 지도 빛 단계를 정하는 값 그대로다. */
    fun percentOf(admCd2: String): Int = percents[admCd2] ?: 0

    /** [percentOf]의 분자 — 그 동에서 "가본 길" 길이(m). */
    fun walkedMetersOf(admCd2: String): Double = walkedLengths[admCd2] ?: 0.0

    /** [percentOf]의 분모 — 그 동 전체 도로 길이(m). */
    fun totalMetersOf(admCd2: String): Double = totalLengths[admCd2] ?: 0.0

    private class BoundsBox(
        val minLat: Double,
        val minLon: Double,
        val maxLat: Double,
        val maxLon: Double,
    )

    private fun boundsOf(parts: List<DongPolygonPart>): BoundsBox? {
        var minLat = Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        var any = false
        for (part in parts) {
            for (point in part.exterior) {
                any = true
                if (point.lat < minLat) minLat = point.lat
                if (point.lat > maxLat) maxLat = point.lat
                if (point.lon < minLon) minLon = point.lon
                if (point.lon > maxLon) maxLon = point.lon
            }
        }
        return if (any) BoundsBox(minLat, minLon, maxLat, maxLon) else null
    }

    companion object {
        /** 동 경계를 못 받았을 때 — 무엇을 물어도 비어 있다고 답한다. */
        fun empty(): DongOverlayQuery =
            DongOverlayQuery(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * iOS 홈 지도의 동/시군구/시도 오버레이 GeoJSON을 만든다 — Android `core:map`의
 * `DongGeoJsonBuilder`/`SigunguGeoJsonBuilder`/`DongAssetRepository`에 해당한다.
 *
 * **Swift가 아니라 Kotlin(iosMain)에 두는 이유**: 전국 경계 파싱·ring 방향 정규화·폴리라벨은 이미
 * commonMain에 있는 순수 로직(`DongBoundaryParser`, `RingWinding`, `Polylabel`)이고, 여기서 하는 일은
 * 그 결과를 GeoJSON 문자열로 엮는 것뿐이다. Swift로 옮기면 같은 로직을 두 번 쓰게 된다. Android가
 * `org.maplibre.geojson`으로 하는 일을 여기서는 kotlinx.serialization으로 한다.
 */
object IosDongOverlayBuilder {

    private val json = Json { ignoreUnknownKeys = true }

    /** `"code":` / `"code" :` / `"code": ` 어느 형태든 잡는다(서버 JSON 포맷이 바뀌어도 깨지지 않게). */
    private val DONG_CODE_PROPERTY_REGEX = Regex("\"code\"\\s*:")

    /**
     * 서버 `index.geojson`은 행정동 코드를 `code` 프로퍼티로 주는데, 번들 에셋과 파서들은 `adm_cd2`를
     * 기대한다 — 값은 서로 같아서(둘 다 10자리 행정동 코드) 키 이름만 바꿔 맞춘다.
     */
    fun normalizeDongCodeKey(rawGeoJson: String): String =
        rawGeoJson.replace(DONG_CODE_PROPERTY_REGEX, "\"adm_cd2\":")

    /**
     * @param dongBoundaryGeoJson 전국 행정동 경계 원본(번들 에셋 또는 서버 `index.geojson`).
     * @param sigunguBoundaryGeoJson 전국 시군구 경계 원본(번들 에셋 전용 — 서버가 안 준다).
     * @param sidoBoundaryGeoJson 전국 시도 경계 원본(번들 에셋 전용).
     * @param regionHierarchyJson 시도 → 시군구 → 행정동 계층 원본.
     * @param dongPercents 행정동 코드 → 탐험률(%). 없는 동은 0%로 본다.
     */
    fun build(
        dongBoundaryGeoJson: String,
        sigunguBoundaryGeoJson: String,
        sidoBoundaryGeoJson: String,
        regionHierarchyJson: String,
        dongPercents: Map<String, Int>,
        /** [dongPercents]의 분자·분모 — 줌인 헤더가 퍼센트와 같은 값으로 거리를 보여준다. */
        walkedLengthsByDong: Map<String, Double>,
        totalLengthsByDong: Map<String, Double>,
        boundarySourceKey: String,
    ): DongOverlayGeoJsonBundle {
        val hierarchy = com.sloth.runveal.dong.region.RegionHierarchyParser.parse(regionHierarchyJson)

        val dongParts = DongBoundaryParser.parsePolygonParts(dongBoundaryGeoJson)
        // 이름만 쓰고 결과는 바로 버린다 — `parse`는 전국 동의 ring을 통째로 들고 있어서, 변수에
        // 담아 두면 `parsePolygonParts` 결과와 **전국 정점을 두 벌** 메모리에 올린 채로 아래
        // GeoJSON 조립까지 간다. iOS는 메모리 한계를 넘으면 크래시 리포트도 없이 프로세스를 죽인다.
        val dongNames = DongBoundaryParser.parse(dongBoundaryGeoJson)
            .associate { it.admCd2 to it.name }

        val sigunguPercents = aggregateBySigungu(dongPercents, hierarchy)
        val sidoPercents = aggregateBySido(sigunguPercents, hierarchy)

        val sigungu = buildRegionOverlay(sigunguBoundaryGeoJson, sigunguPercents)
        val sido = buildRegionOverlay(sidoBoundaryGeoJson, sidoPercents)

        return DongOverlayGeoJsonBundle(
            dongFillGeoJson = buildFillGeoJson(dongBoundaryGeoJson, dongPercents),
            dongLineGeoJson = buildLineGeoJson(dongParts, dongPercents),
            dongLabelGeoJson = buildDongLabelGeoJson(dongParts, dongNames, dongPercents),
            sigunguLineGeoJson = sigungu.lineGeoJson,
            sigunguLabelGeoJson = sigungu.labelGeoJson,
            sidoLineGeoJson = sido.lineGeoJson,
            sidoLabelGeoJson = sido.labelGeoJson,
            query = DongOverlayQuery(
                partsByAdmCd2 = dongParts,
                levelByAdmCd2 = dongParts.keys.associateWith { levelOf(dongPercents[it] ?: 0) },
                percents = dongPercents,
                walkedLengths = walkedLengthsByDong,
                totalLengths = totalLengthsByDong,
            ),
            boundarySourceKey = boundarySourceKey,
        )
    }

    private data class RegionOverlay(val lineGeoJson: String, val labelGeoJson: String)

    /** 시군구·시도는 입력(경계 원본, percents)만 다르고 만드는 모양이 완전히 같다 — fill은 안 만든다. */
    private fun buildRegionOverlay(rawGeoJson: String, percents: Map<String, Int>): RegionOverlay {
        val parts = DongBoundaryParser.parsePolygonParts(rawGeoJson)
        // 동 쪽과 같은 이유로 이름만 뽑고 버린다([build] 주석).
        val names = DongBoundaryParser.parse(rawGeoJson)
            .associate { it.admCd2 to it.name }
        return RegionOverlay(
            lineGeoJson = buildLineGeoJson(parts, percents),
            labelGeoJson = buildNameLabelGeoJson(parts, names, percents),
        )
    }

    // ---------- fill / line / label ----------

    /** 원본 feature를 그대로 두고 `level`/`percentValue` 프로퍼티만 붙인다. */
    private fun buildFillGeoJson(rawGeoJson: String, percents: Map<String, Int>): String {
        val root = json.parseToJsonElement(rawGeoJson).jsonObject
        val features = root["features"]?.jsonArray ?: return emptyFeatureCollection()
        val mapped = features.mapNotNull { element ->
            val feature = element.jsonObject
            val properties = feature["properties"]?.jsonObject ?: return@mapNotNull null
            val admCd2 = properties["adm_cd2"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val geometry = feature["geometry"] ?: return@mapNotNull null
            val percent = percents[admCd2] ?: 0
            buildJsonObject {
                put("type", "Feature")
                put("geometry", geometry)
                put(
                    "properties",
                    buildJsonObject {
                        properties.forEach { (key, value) -> put(key, value) }
                        put("level", levelOf(percent))
                        put("percentValue", percent.toString())
                    },
                )
            }
        }
        return featureCollection(mapped)
    }

    /**
     * 경계를 공유하는 두 지역의 선이 같은 자리에 겹쳐 그려지는데, MapLibre는 피처 배열 순서대로
     * (뒤에 올수록 위에) 그린다. level 오름차순으로 정렬해서 레벨이 높은 쪽 선이 항상 위에 오게 한다.
     */
    private fun buildLineGeoJson(
        partsByCode: Map<String, List<DongPolygonPart>>,
        percents: Map<String, Int>,
    ): String {
        val features = mutableListOf<JsonObject>()
        partsByCode.entries
            .sortedBy { (code, _) -> levelOf(percents[code] ?: 0) }
            .forEach { (code, parts) ->
                val level = levelOf(percents[code] ?: 0)
                parts.forEach { part ->
                    (listOf(part.exterior) + part.holes).forEach { ring ->
                        features += lineFeature(ring, code, level)
                    }
                }
            }
        return featureCollection(features)
    }

    private fun buildDongLabelGeoJson(
        partsByCode: Map<String, List<DongPolygonPart>>,
        namesByCode: Map<String, String>,
        percents: Map<String, Int>,
    ): String {
        val features = partsByCode.mapNotNull { (code, parts) ->
            val name = namesByCode[code] ?: return@mapNotNull null
            val anchor = labelAnchor(parts) ?: return@mapNotNull null
            val percent = percents[code] ?: 0
            pointFeature(anchor) {
                put("adm_cd2", code)
                put("name", name)
                put("level", levelOf(percent))
                put("percentValue", percent.toString())
                // 라벨끼리 겹칠 때 탐험률 높은 쪽을 살리는 데 쓴다(라벨 레이어의 symbolSortKey).
                // 표시용 `percentValue`는 문자열이라 계산에 못 쓴다.
                put("percent", percent)
                // iOS 라벨 레이어는 `Expression.format`(토큰마다 다른 색)을 쓸 수 없어서 한 레이어에
                // 문자열 하나만 넣는다 — 두 줄 라벨을 여기서 미리 만들어 둔다.
                put("label", name + "\n" + percent + "%")
            }
        }
        return featureCollection(features)
    }

    /** [percents]를 라벨에도 실어 보낸다 — 겹칠 때 탐험률 높은 쪽을 살리는 데 쓴다(symbolSortKey). */
    private fun buildNameLabelGeoJson(
        partsByCode: Map<String, List<DongPolygonPart>>,
        namesByCode: Map<String, String>,
        percents: Map<String, Int>,
    ): String {
        val features = partsByCode.mapNotNull { (code, parts) ->
            val name = namesByCode[code] ?: return@mapNotNull null
            val anchor = labelAnchor(parts) ?: return@mapNotNull null
            pointFeature(anchor) {
                put("adm_cd2", code)
                put("name", name)
                put("percent", percents[code] ?: 0)
            }
        }
        return featureCollection(features)
    }

    /** 가장 넓은 조각의 내접원 중심(폴리라벨)에 라벨을 둔다. */
    private fun labelAnchor(parts: List<DongPolygonPart>): LatLon? {
        val largest = parts.maxByOrNull { ringAreaAbs(it.exterior) } ?: return null
        return Polylabel.pole(largest.exterior, largest.holes)
    }

    // ---------- 집계 ----------

    /**
     * 시군구 탐험률 = 소속 행정동 탐험률의 평균(동이 없으면 0). 올림(ceiling)이라 소속 동 중 하나라도
     * 탐험률이 있으면 평균이 0으로 뭉개지지 않는다.
     */
    private fun aggregateBySigungu(dongPercents: Map<String, Int>, hierarchy: RegionHierarchy): Map<String, Int> =
        hierarchy.sido.flatMap { it.sigungu }.associate { sigungu ->
            val dongCodes = sigungu.dongs.map { it.code }
            val sum = dongCodes.sumOf { dongPercents[it] ?: 0 }
            sigungu.code to ceilDivide(sum, dongCodes.size)
        }

    /** 시도 탐험률 = 소속 시군구 탐험률의 평균. [aggregateBySigungu]와 같은 이유로 올림. */
    private fun aggregateBySido(sigunguPercents: Map<String, Int>, hierarchy: RegionHierarchy): Map<String, Int> =
        hierarchy.sido.associate { sido ->
            val sigunguCodes = sido.sigungu.map { it.code }
            val sum = sigunguCodes.sumOf { sigunguPercents[it] ?: 0 }
            sido.code to ceilDivide(sum, sigunguCodes.size)
        }

    private fun ceilDivide(sum: Int, count: Int): Int = if (count <= 0) 0 else (sum + count - 1) / count

    // ---------- 단계 ----------

    /**
     * 정수 탐험률(%)을 단계(0..6)로 바꾼다 — Android `RunvealDong.levelOf`,
     * iOS `RunvealDong.levelOf`(iosApp/iosApp/DesignSystem/RunvealDong.swift)와 **같은 경계값**이어야 한다.
     * 지도 레이어의 `level` 피처 프로퍼티를 여기서 만들기 때문에 Kotlin 쪽에도 한 벌 필요하다.
     */
    private fun levelOf(ratePercent: Int): Int = when {
        ratePercent < 1 -> 0
        ratePercent < 5 -> 1
        ratePercent < 10 -> 2
        ratePercent < 20 -> 3
        ratePercent < 40 -> 4
        ratePercent < 70 -> 5
        else -> 6
    }

    // ---------- GeoJSON 조립 ----------

    private fun lineFeature(ring: List<LatLon>, code: String, level: Int): JsonObject = buildJsonObject {
        put("type", "Feature")
        put(
            "geometry",
            buildJsonObject {
                put("type", "LineString")
                put("coordinates", coordinateArray(ring))
            },
        )
        put(
            "properties",
            buildJsonObject {
                put("adm_cd2", code)
                put("level", level)
            },
        )
    }

    private fun pointFeature(point: LatLon, properties: JsonObjectBuilderScope): JsonObject = buildJsonObject {
        put("type", "Feature")
        put(
            "geometry",
            buildJsonObject {
                put("type", "Point")
                put(
                    "coordinates",
                    buildJsonArray {
                        add(JsonPrimitive(point.lon))
                        add(JsonPrimitive(point.lat))
                    },
                )
            },
        )
        put("properties", buildJsonObject(properties))
    }

    private fun coordinateArray(ring: List<LatLon>): JsonArray = buildJsonArray {
        ring.forEach { p ->
            add(
                buildJsonArray {
                    add(JsonPrimitive(p.lon))
                    add(JsonPrimitive(p.lat))
                },
            )
        }
    }

    private fun featureCollection(features: List<JsonObject>): String {
        val collection = buildJsonObject {
            put("type", "FeatureCollection")
            put("features", JsonArray(features))
        }
        return json.encodeToString(JsonObject.serializer(), collection)
    }

    fun emptyFeatureCollection(): String = """{"type":"FeatureCollection","features":[]}"""

    private fun ringAreaAbs(ring: List<LatLon>): Double {
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a.lon * b.lat - b.lon * a.lat
        }
        return abs(sum) / 2.0
    }
}

private typealias JsonObjectBuilderScope = kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
