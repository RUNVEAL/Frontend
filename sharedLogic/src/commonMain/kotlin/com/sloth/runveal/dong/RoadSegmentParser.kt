package com.sloth.runveal.dong

import com.sloth.runveal.map.LatLon
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val LINES_SERIALIZER = ListSerializer(ListSerializer(ListSerializer(Double.serializer())))

/** 서버 지도 파일 `segments-{구코드}.geojson`(`docs/api/API.md` §3.1.2)을 [RoadSegment] 목록으로 읽는다. */
object RoadSegmentParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(geoJsonText: String): List<RoadSegment> =
        json.decodeFromString<SegmentFeatureCollectionJson>(geoJsonText).features.map { feature ->
            RoadSegment(
                segmentId = feature.properties.segmentId,
                lengthM = feature.properties.lengthM,
                lines = feature.geometry.coordinates.map { line -> line.map { LatLon(lat = it[1], lon = it[0]) } },
            )
        }

    /** [RoadSegment.lines]를 로컬 DB `road_segment.coordinates`에 저장할 문자열로. [decodeLines]로
     * 그대로 되돌린다 — `MultiLineString`의 조각 구조(끊긴 선)를 유지해야 [SegmentCoverageMatcher]가
     * 조각 사이를 이어진 선으로 착각하지 않는다. */
    fun encodeLines(lines: List<List<LatLon>>): String =
        json.encodeToString(LINES_SERIALIZER, lines.map { line -> line.map { listOf(it.lon, it.lat) } })

    fun decodeLines(text: String): List<List<LatLon>> =
        json.decodeFromString(LINES_SERIALIZER, text).map { line -> line.map { LatLon(lat = it[1], lon = it[0]) } }
}

@Serializable
private data class SegmentFeatureCollectionJson(val features: List<SegmentFeatureJson> = emptyList())

@Serializable
private data class SegmentFeatureJson(val properties: SegmentPropertiesJson, val geometry: SegmentGeometryJson)

@Serializable
private data class SegmentPropertiesJson(val segmentId: Long, val lengthM: Int)

/** `MultiLineString`이라 좌표가 "조각 목록의 좌표 목록"으로 한 겹 더 감싸여 있다. */
@Serializable
private data class SegmentGeometryJson(val coordinates: List<List<List<Double>>>)
