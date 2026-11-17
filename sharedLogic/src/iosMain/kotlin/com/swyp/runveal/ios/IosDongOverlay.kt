package com.swyp.runveal.ios

import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.Polylabel
import com.swyp.runveal.dong.region.RegionHierarchy
import com.swyp.runveal.map.LatLon
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
    /** 빛 이미지도 테두리 선과 **같은 정점**을 쓰도록 같이 돌려준다 — 경계 GeoJSON을 따로 한 번 더
     * 파싱하면 전국 데이터를 두 번 읽게 되고, 전처리(ring 방향 정규화)가 어긋날 여지도 생긴다. */
    val dongPolygonPartsByAdmCd2: Map<String, List<DongPolygonPart>>,
    /** 행정동 코드 → 단계(0~6). 빛 이미지 레이어가 "어느 동에 몇 단계 이미지를 올릴지" 판단할 때 쓴다 —
     * Android는 fill GeoJSON을 다시 파싱해서 뽑지만(4MB짜리 재파싱이 팬·줌 버벅임의 원인이었다),
     * 여기서는 만들 때 이미 아는 값이라 그대로 같이 돌려준다. */
    val dongLevelByAdmCd2: Map<String, Int>,
    /** 이 오버레이를 만든 경계 데이터의 출처 — `"server:<지도 버전>"`(경계를 못 받았으면 `"none"`).
     * 빛 이미지 디스크 캐시 키에 들어가서, 서버가 경계를 고쳐 새 버전으로 배포하면 옛 경계로
     * 만들어 둔 빛 이미지가 자동으로 버려진다(안 그러면 새 테두리와 어긋난 채로 남는다). */
    val boundarySourceKey: String,
) {
    companion object {
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
                dongPolygonPartsByAdmCd2 = emptyMap(),
                dongLevelByAdmCd2 = emptyMap(),
                boundarySourceKey = "none",
            )
        }
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
        boundarySourceKey: String,
    ): DongOverlayGeoJsonBundle {
        val hierarchy = com.swyp.runveal.dong.region.RegionHierarchyParser.parse(regionHierarchyJson)

        val dongBoundaries = DongBoundaryParser.parse(dongBoundaryGeoJson)
        val dongParts = DongBoundaryParser.parsePolygonParts(dongBoundaryGeoJson)
        val dongNames = dongBoundaries.associate { it.admCd2 to it.name }

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
            dongPolygonPartsByAdmCd2 = dongParts,
            dongLevelByAdmCd2 = dongParts.keys.associateWith { levelOf(dongPercents[it] ?: 0) },
            boundarySourceKey = boundarySourceKey,
        )
    }

    private data class RegionOverlay(val lineGeoJson: String, val labelGeoJson: String)

    /** 시군구·시도는 입력(경계 원본, percents)만 다르고 만드는 모양이 완전히 같다 — fill은 안 만든다. */
    private fun buildRegionOverlay(rawGeoJson: String, percents: Map<String, Int>): RegionOverlay {
        val boundaries = DongBoundaryParser.parse(rawGeoJson)
        val parts = DongBoundaryParser.parsePolygonParts(rawGeoJson)
        val names = boundaries.associate { it.admCd2 to it.name }
        return RegionOverlay(
            lineGeoJson = buildLineGeoJson(parts, percents),
            labelGeoJson = buildNameLabelGeoJson(parts, names),
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
                // iOS 라벨 레이어는 `Expression.format`(토큰마다 다른 색)을 쓸 수 없어서 한 레이어에
                // 문자열 하나만 넣는다 — 두 줄 라벨을 여기서 미리 만들어 둔다.
                put("label", name + "\n" + percent + "%")
            }
        }
        return featureCollection(features)
    }

    private fun buildNameLabelGeoJson(
        partsByCode: Map<String, List<DongPolygonPart>>,
        namesByCode: Map<String, String>,
    ): String {
        val features = partsByCode.mapNotNull { (code, parts) ->
            val name = namesByCode[code] ?: return@mapNotNull null
            val anchor = labelAnchor(parts) ?: return@mapNotNull null
            pointFeature(anchor) {
                put("adm_cd2", code)
                put("name", name)
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
