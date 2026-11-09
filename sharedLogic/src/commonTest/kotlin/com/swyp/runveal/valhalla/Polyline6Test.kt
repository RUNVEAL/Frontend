package com.swyp.runveal.valhalla

import com.swyp.runveal.map.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val EPSILON = 1e-5

class Polyline6Test {

    @Test
    fun decodeOfEncodeReturnsOriginalCoordinates() {
        val original = listOf(
            LatLon(lat = 37.5665, lon = 126.9780),
            LatLon(lat = 37.5670, lon = 126.9790),
            LatLon(lat = 37.5650, lon = 126.9770),
        )

        val decoded = Polyline6.decode(Polyline6.encode(original))

        assertEquals(original.size, decoded.size)
        original.zip(decoded).forEach { (expected, actual) ->
            assertTrue(abs(expected.lat - actual.lat) < EPSILON)
            assertTrue(abs(expected.lon - actual.lon) < EPSILON)
        }
    }

    @Test
    fun encodeOfEmptyListIsEmptyString() {
        assertEquals("", Polyline6.encode(emptyList()))
    }

    @Test
    fun decodeOfEmptyStringIsEmptyList() {
        assertTrue(Polyline6.decode("").isEmpty())
    }

    @Test
    fun handlesNegativeCoordinates() {
        val original = listOf(LatLon(lat = -33.8688, lon = -70.6693))

        val decoded = Polyline6.decode(Polyline6.encode(original))

        assertEquals(1, decoded.size)
        assertTrue(abs(original[0].lat - decoded[0].lat) < EPSILON)
        assertTrue(abs(original[0].lon - decoded[0].lon) < EPSILON)
    }
}
