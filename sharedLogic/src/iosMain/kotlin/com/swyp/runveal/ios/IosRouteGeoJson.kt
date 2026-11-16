package com.swyp.runveal.ios

import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.LatLon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 지도에 그릴 추천 코스 하나. [points]가 2개 미만이면 선으로 그려지지 않는다. */
data class IosRecommendedRoute(val id: String, val points: List<LatLon>, val selected: Boolean)

/**
 * 지도 선 레이어에 넣을 GeoJSON을 만든다 — Android `core:map`의 `VisitedRoadGeoJson`,
 * `VisitedRoadGeoJsonBuilder`, `RecommendedRouteGeoJson`에 해당한다.
 *
 * Swift가 아니라 Kotlin(iosMain)에 두는 이유: 입력이 전부 sharedLogic 타입(`LatLon`, `MatchedEdge`)이고,
 * 하는 일은 좌표 배열을 GeoJSON 문자열로 엮는 것뿐이다.
 */
object IosRouteGeoJson {

    val empty: String = IosDongOverlayBuilder.emptyFeatureCollection()

    /** [routes](각각 끊어진 경로 하나)를 "가본 길"/"오늘 달린 길" 선 레이어용 GeoJSON으로. */
    fun lines(routes: List<List<LatLon>>): String =
        featureCollection(
            routes.filter { it.size >= 2 }.map { points -> lineFeature(points, properties = null) },
        )

    /** 매칭된 구간([MatchedEdge]) 목록을 "가본 길" 선 레이어용 GeoJSON으로. */
    fun matchedEdges(edges: List<MatchedEdge>): String =
        lines(edges.map { it.coordinates })

    /** 추천 코스 — 선택 여부가 `selected` 피처 속성으로 들어가고, 레이어가 그 속성으로 필터링한다. */
    fun recommendedRoutes(routes: List<IosRecommendedRoute>): String =
        featureCollection(
            routes.filter { it.points.size >= 2 }.map { route ->
                lineFeature(route.points, properties = { put("selected", route.selected) })
            },
        )

    /** 러닝 중 코스 안내선(흰 점선) — 한 줄뿐이라 속성이 없다. */
    fun courseGuide(path: List<LatLon>): String = lines(listOf(path))

    private fun lineFeature(
        points: List<LatLon>,
        properties: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)?,
    ): JsonObject = buildJsonObject {
        put("type", "Feature")
        put(
            "geometry",
            buildJsonObject {
                put("type", "LineString")
                put(
                    "coordinates",
                    buildJsonArray {
                        points.forEach { p ->
                            add(
                                buildJsonArray {
                                    add(JsonPrimitive(p.lon))
                                    add(JsonPrimitive(p.lat))
                                },
                            )
                        }
                    },
                )
            },
        )
        put("properties", buildJsonObject { properties?.invoke(this) })
    }

    private fun featureCollection(features: List<JsonObject>): String {
        val collection = buildJsonObject {
            put("type", "FeatureCollection")
            put("features", JsonArray(features))
        }
        return kotlinx.serialization.json.Json.encodeToString(JsonObject.serializer(), collection)
    }
}
