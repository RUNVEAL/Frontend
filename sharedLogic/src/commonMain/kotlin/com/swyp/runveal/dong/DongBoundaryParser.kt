package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon
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

    private fun parseFeature(feature: JsonObject): DongBoundary? {
        val properties = feature["properties"]?.jsonObject ?: return null
        val admCd2 = properties["adm_cd2"]?.jsonPrimitive?.content ?: return null
        val name = properties["name"]?.jsonPrimitive?.content ?: admCd2
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
