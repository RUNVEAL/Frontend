package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `<trackId>.matched.geojson`(번들 샘플 트랙, `docs/map/local-db-schema-design.md` §4)을
 * [MatchedEdge] 목록으로 읽는다. `wayId`/`unmatchedPointCount`/`confidenceScore`는 지금은 쓰지
 * 않아서 무시한다. */
object MatchedEdgeParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): List<MatchedEdge> =
        json.decodeFromString<MatchedTrackJson>(jsonText).features.map { feature ->
            MatchedEdge(coordinates = feature.geometry.coordinates.map { LatLon(lat = it[1], lon = it[0]) })
        }
}

@Serializable
private data class MatchedTrackJson(val features: List<MatchedFeatureJson> = emptyList())

@Serializable
private data class MatchedFeatureJson(val geometry: MatchedGeometryJson)

@Serializable
private data class MatchedGeometryJson(val coordinates: List<List<Double>>)
