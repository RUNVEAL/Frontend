package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.Polylabel
import com.swyp.runveal.dong.RingWinding
import com.swyp.runveal.map.LatLon
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/**
 * 테두리/라벨만 있는 개요용 경계 GeoJSON(line+label)을 만든다. [DongGeoJsonBuilder]와 같은 방식
 * (ring 추출, Polylabel로 라벨 위치 계산)이지만 완주율 단계(level)/퍼센트 개념이 없어 fill은
 * 만들지 않는다. 시군구·시도 둘 다 이 빌더를 그대로 쓴다 — 입력(boundaries, 원본 GeoJSON)만 다르고
 * 만드는 모양이 완전히 같다.
 */
internal object SigunguGeoJsonBuilder {

    data class Result(val lineGeoJson: String, val labelGeoJson: String)

    fun build(boundaries: List<DongBoundary>, rawBoundaryGeoJson: String): Result {
        val raw = FeatureCollection.fromJson(rawBoundaryGeoJson)
        val partsByAdmCd2 = polygonPartsByAdmCd2(raw)
        val nameByAdmCd2 = boundaries.associate { it.admCd2 to it.name }

        return Result(
            lineGeoJson = buildLineGeoJson(partsByAdmCd2),
            labelGeoJson = buildLabelGeoJson(partsByAdmCd2, nameByAdmCd2),
        )
    }

    private fun buildLineGeoJson(partsByAdmCd2: Map<String, List<DongPolygonPart>>): String {
        val features = mutableListOf<Feature>()
        partsByAdmCd2.forEach { (admCd2, parts) ->
            parts.forEach { part ->
                (listOf(part.exterior) + part.holes).forEach { ring ->
                    val line = Feature.fromGeometry(LineString.fromLngLats(ring.map { Point.fromLngLat(it.lon, it.lat) }))
                    line.addStringProperty("adm_cd2", admCd2)
                    features += line
                }
            }
        }
        return FeatureCollection.fromFeatures(features).toJson()
    }

    private fun buildLabelGeoJson(
        partsByAdmCd2: Map<String, List<DongPolygonPart>>,
        nameByAdmCd2: Map<String, String>,
    ): String {
        val features = partsByAdmCd2.mapNotNull { (admCd2, parts) ->
            val name = nameByAdmCd2[admCd2] ?: return@mapNotNull null
            val largest = parts.maxByOrNull { ringAreaAbs(it.exterior) } ?: return@mapNotNull null
            val anchor = Polylabel.pole(largest.exterior, largest.holes)
            Feature.fromGeometry(Point.fromLngLat(anchor.lon, anchor.lat)).apply {
                addStringProperty("adm_cd2", admCd2)
                addStringProperty("name", name)
            }
        }
        return FeatureCollection.fromFeatures(features).toJson()
    }

    private fun polygonPartsByAdmCd2(raw: FeatureCollection): Map<String, List<DongPolygonPart>> {
        val result = mutableMapOf<String, MutableList<DongPolygonPart>>()
        raw.features()?.forEach { feature ->
            val admCd2 = feature.getStringProperty("adm_cd2") ?: return@forEach
            polygonsOf(feature).forEach { polygon ->
                val rings = polygon.coordinates().map { ring -> ring.map { LatLon(lat = it.latitude(), lon = it.longitude()) } }
                val part = RingWinding.normalize(exterior = rings.first(), holes = rings.drop(1))
                result.getOrPut(admCd2) { mutableListOf() }.add(part)
            }
        }
        return result
    }

    private fun polygonsOf(feature: Feature): List<Polygon> = when (val geometry = feature.geometry()) {
        is Polygon -> listOf(geometry)
        is MultiPolygon -> geometry.polygons()
        else -> emptyList()
    }

    private fun ringAreaAbs(ring: List<LatLon>): Double {
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a.lon * b.lat - b.lon * a.lat
        }
        return kotlin.math.abs(sum) / 2.0
    }
}
