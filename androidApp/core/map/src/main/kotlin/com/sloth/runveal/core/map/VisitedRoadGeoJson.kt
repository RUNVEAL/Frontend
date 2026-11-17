package com.sloth.runveal.core.map

import com.sloth.runveal.map.LatLon
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** [routes](각각 끊어진 경로 하나)를 [RunvealMap]의 `visitedRoadGeoJson`에 넘길 GeoJSON 문자열로 바꾼다. */
fun buildVisitedRoadGeoJson(routes: List<List<LatLon>>): String {
    val features = routes.filter { it.size >= 2 }.map { points ->
        Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) }))
    }
    return FeatureCollection.fromFeatures(features).toJson()
}
