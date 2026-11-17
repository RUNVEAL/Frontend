package com.sloth.runveal.dong

import com.sloth.runveal.map.LatLon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * assets의 행정동 경계 GeoJSON을 파싱한다. 지도 SDK의 FeatureCollection이 아니라 순수
 * kotlinx.serialization의 JsonElement 트리로 직접 읽어서(플랫폼 의존 없음) commonTest가 가능하다.
 */
object DongBoundaryParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(geoJsonText: String): List<DongBoundary> {
        val root = json.parseToJsonElement(geoJsonText).jsonObject
        val features = root["features"]?.jsonArray ?: return emptyList()
        return features.mapNotNull { parseFeature(it.jsonObject) }
    }

    /**
     * [parse]와 달리 구멍(hole)을 보존해서 `adm_cd2` → [DongPolygonPart] 목록으로 돌려준다. 동 빛
     * 이미지 생성(폴리라벨, 마스크)처럼 구멍이 실제로 필요한 계산에서만 쓴다. 외곽/구멍 ring 방향은
     * [RingWinding.normalize]로 맞춘다.
     */
    fun parsePolygonParts(geoJsonText: String): Map<String, List<DongPolygonPart>> {
        val root = json.parseToJsonElement(geoJsonText).jsonObject
        val features = root["features"]?.jsonArray ?: return emptyMap()
        val result = mutableMapOf<String, MutableList<DongPolygonPart>>()
        features.forEach { feature ->
            val properties = feature.jsonObject["properties"]?.jsonObject ?: return@forEach
            val admCd2 = properties["adm_cd2"]?.jsonPrimitive?.content ?: return@forEach
            val geometry = feature.jsonObject["geometry"]?.jsonObject ?: return@forEach
            val parts = parsePolygonParts(geometry)
            if (parts.isNotEmpty()) result.getOrPut(admCd2) { mutableListOf() }.addAll(parts)
        }
        return result
    }

    private fun parsePolygonParts(geometry: JsonObject): List<DongPolygonPart> {
        val type = geometry["type"]?.jsonPrimitive?.content ?: return emptyList()
        val coordinates = geometry["coordinates"]?.jsonArray ?: return emptyList()
        return when (type) {
            "Polygon" -> listOf(toPolygonPart(coordinates))
            "MultiPolygon" -> coordinates.map { polygon -> toPolygonPart(polygon.jsonArray) }
            else -> emptyList()
        }
    }

    /** [polygonCoords]는 ring 배열(첫 번째 = 외곽, 나머지 = 구멍)이다. */
    private fun toPolygonPart(polygonCoords: JsonArray): DongPolygonPart {
        val rings = polygonCoords.map { ring -> parseRing(ring.jsonArray) }
        return RingWinding.normalize(exterior = rings.first(), holes = rings.drop(1))
    }

    private fun parseFeature(feature: JsonObject): DongBoundary? {
        val properties = feature["properties"]?.jsonObject ?: return null
        val admCd2 = properties["adm_cd2"]?.jsonPrimitive?.content ?: return null
        // 번들 에셋은 "사직동"처럼 동 이름만 주지만, 서버 index.geojson은 "서울특별시 종로구 사직동"처럼
        // 시/도·시/군/구까지 붙여서 준다 — 지도 라벨엔 동 이름만 보여야 해서 마지막 토큰만 쓴다.
        val name = properties["name"]?.jsonPrimitive?.content?.substringAfterLast(' ') ?: admCd2
        val geometry = feature["geometry"]?.jsonObject ?: return null
        val rings = parseRings(geometry)
        if (rings.isEmpty()) return null
        return DongBoundary(admCd2 = admCd2, name = name, rings = rings)
    }

    private fun parseRings(geometry: JsonObject): List<List<LatLon>> {
        val type = geometry["type"]?.jsonPrimitive?.content ?: return emptyList()
        val coordinates = geometry["coordinates"]?.jsonArray ?: return emptyList()
        return when (type) {
            "Polygon" -> listOfNotNull(coordinates.firstOrNull()?.jsonArray?.let(::parseRing))
            "MultiPolygon" -> coordinates.mapNotNull { polygon ->
                polygon.jsonArray.firstOrNull()?.jsonArray?.let(::parseRing)
            }
            else -> emptyList()
        }
    }

    private fun parseRing(ring: JsonArray): List<LatLon> =
        ring.map { point ->
            val coords = point.jsonArray
            LatLon(lat = coords[1].jsonPrimitive.double, lon = coords[0].jsonPrimitive.double)
        }
}
