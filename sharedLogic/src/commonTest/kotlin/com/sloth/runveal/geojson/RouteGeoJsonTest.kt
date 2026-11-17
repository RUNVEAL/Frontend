package com.sloth.runveal.geojson

import com.sloth.runveal.map.LatLon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteGeoJsonTest {

    @Test
    fun coordinatesAreInLngLatOrder() {
        val route = listOf(LatLon(lat = 37.5665, lon = 126.9780), LatLon(lat = 37.5700, lon = 126.9800))

        val json = Json.parseToJsonElement(route.toRouteGeoJson()).jsonObject
        val feature = json["features"]!!.jsonArray[0].jsonObject
        val coordinates = feature["geometry"]!!.jsonObject["coordinates"]!!.jsonArray

        val first = coordinates[0].jsonArray
        assertEquals(126.9780, first[0].jsonPrimitive.double)
        assertEquals(37.5665, first[1].jsonPrimitive.double)
    }


    @Test
    fun producesOneLineStringFeaturePerRoute() {
        val route = listOf(LatLon(37.0, 127.0), LatLon(37.001, 127.001), LatLon(37.002, 127.002))

        val json = Json.parseToJsonElement(route.toRouteGeoJson()).jsonObject
        assertEquals("FeatureCollection", json["type"]!!.jsonPrimitive.content)

        val features = json["features"]!!.jsonArray
        assertEquals(1, features.size)
        val geometry = features[0].jsonObject["geometry"]!!.jsonObject
        assertEquals("LineString", geometry["type"]!!.jsonPrimitive.content)
        assertEquals(3, geometry["coordinates"]!!.jsonArray.size)
    }
}
