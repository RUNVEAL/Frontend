package com.sloth.runveal.geojson

import com.sloth.runveal.map.LatLon
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 경로 한 줄을 GeoJSON `FeatureCollection`(LineString Feature 1개) 문자열로 바꾼다. 좌표 순서는
 * GeoJSON 표준대로 `[lng, lat]`이다(`core:map`의 `VisitedRoadGeoJson.kt`와 같은 좌표 순서, 다만 거긴
 * MapLibre Android의 `org.maplibre.geojson.*` 타입을 쓰는 Android 전용 코드라 commonMain에서는
 * 재사용할 수 없어 순수 문자열로 새로 둔다). */
fun List<LatLon>.toRouteGeoJson(): String {
    val geometry: JsonObject = buildJsonObject {
        put("type", "LineString")
        put(
            "coordinates",
            buildJsonArray {
                this@toRouteGeoJson.forEach { point ->
                    addJsonArray {
                        add(point.lon)
                        add(point.lat)
                    }
                }
            },
        )
    }
    val feature = buildJsonObject {
        put("type", "Feature")
        put("properties", buildJsonObject {})
        put("geometry", geometry)
    }
    val featureCollection = buildJsonObject {
        put("type", "FeatureCollection")
        put("features", buildJsonArray { add(feature) })
    }
    return featureCollection.toString()
}

/** 여러 줄(서로 떨어져 있어도 된다)을 GeoJSON `MultiLineString` Feature 하나로 묶는다 — 러닝 결과
 * 카드에서 "이번에 처음 간 구간"들만 골라 금색으로 덧그릴 때 쓴다. 점이 2개 미만인 줄은 버린다. */
fun List<List<LatLon>>.toMultiLineGeoJson(): String {
    val lines = filter { it.size >= 2 }
    val geometry: JsonObject = buildJsonObject {
        put("type", "MultiLineString")
        put(
            "coordinates",
            buildJsonArray {
                lines.forEach { line ->
                    addJsonArray {
                        line.forEach { point ->
                            addJsonArray {
                                add(point.lon)
                                add(point.lat)
                            }
                        }
                    }
                }
            },
        )
    }
    val feature = buildJsonObject {
        put("type", "Feature")
        put("properties", buildJsonObject {})
        put("geometry", geometry)
    }
    return buildJsonObject {
        put("type", "FeatureCollection")
        put("features", buildJsonArray { add(feature) })
    }.toString()
}
