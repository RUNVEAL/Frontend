package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.dong.MatchedEdge
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** [MatchedEdge] 목록을 [VisitedRoadLayer]가 쓸 LineString GeoJSON으로 바꾼다. */
internal object VisitedRoadGeoJsonBuilder {
    fun build(edges: List<MatchedEdge>): String {
        val features = edges
            .filter { it.coordinates.size >= 2 }
            .map { edge -> Feature.fromGeometry(LineString.fromLngLats(edge.coordinates.map { Point.fromLngLat(it.lon, it.lat) })) }
        return FeatureCollection.fromFeatures(features).toJson()
    }
}
