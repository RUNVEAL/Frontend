package com.sloth.runveal.core.map

import com.sloth.runveal.map.LatLon
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** 지도에 그릴 추천 코스 하나. [points]가 2개 미만이면 선으로 그려지지 않는다. */
data class RecommendedRoute(val id: String, val points: List<LatLon>, val selected: Boolean)

/** [routes]를 [RunvealMap]의 추천 코스 레이어에 넘길 GeoJSON 문자열로 바꾼다. 선택 여부는
 * `selected` 피처 속성으로 들어가고, 지도 쪽 레이어가 이 속성으로 필터링해서 선택된 코스만 강조한다. */
fun buildRecommendedRouteGeoJson(routes: List<RecommendedRoute>): String {
    val features = routes.filter { it.points.size >= 2 }.map { route ->
        Feature.fromGeometry(LineString.fromLngLats(route.points.map { Point.fromLngLat(it.lon, it.lat) })).apply {
            addBooleanProperty("selected", route.selected)
        }
    }
    return FeatureCollection.fromFeatures(features).toJson()
}
