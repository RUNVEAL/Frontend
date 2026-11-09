package com.swyp.runveal.valhalla

import com.swyp.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals

class TraceResponseMapperTest {

    @Test
    fun decodesShapeAndSlicesEdgesByIndex() {
        val shape = Polyline6.encode(
            listOf(
                LatLon(lat = 37.5665, lon = 126.9780),
                LatLon(lat = 37.5666, lon = 126.9781),
                LatLon(lat = 37.5667, lon = 126.9782),
            ),
        )
        val response = TraceResponse(
            shape = shape,
            edges = listOf(EdgeDto(wayId = 42L, beginShapeIndex = 0, endShapeIndex = 1)),
            confidenceScore = 0.9,
        )

        val result = TraceResponseMapper.toMatchResult(response)

        assertEquals(3, result.path.size)
        assertEquals(1, result.edges.size)
        assertEquals(42L, result.edges[0].wayId)
        assertEquals(2, result.edges[0].coordinates.size)
        assertEquals(0.9, result.confidenceScore)
    }

    @Test
    fun dropsEdgeWithMissingIndex() {
        val shape = Polyline6.encode(listOf(LatLon(lat = 37.5665, lon = 126.9780)))
        val response = TraceResponse(
            shape = shape,
            edges = listOf(EdgeDto(wayId = 1L, beginShapeIndex = null, endShapeIndex = 0)),
        )

        val result = TraceResponseMapper.toMatchResult(response)

        assertEquals(0, result.edges.size)
    }

    @Test
    fun dropsEdgeWithOutOfRangeIndex() {
        val shape = Polyline6.encode(listOf(LatLon(lat = 37.5665, lon = 126.9780)))
        val response = TraceResponse(
            shape = shape,
            edges = listOf(EdgeDto(wayId = 1L, beginShapeIndex = 0, endShapeIndex = 5)),
        )

        val result = TraceResponseMapper.toMatchResult(response)

        assertEquals(0, result.edges.size)
    }

    @Test
    fun collectsOnlyUnmatchedPoints() {
        val response = TraceResponse(
            shape = null,
            matchedPoints = listOf(
                MatchedPointDto(lat = 1.0, lon = 1.0, type = "matched"),
                MatchedPointDto(lat = 2.0, lon = 2.0, type = "unmatched"),
                MatchedPointDto(lat = 3.0, lon = 3.0, type = "interpolated"),
            ),
        )

        val result = TraceResponseMapper.toMatchResult(response)

        assertEquals(1, result.unmatchedPointCount)
        assertEquals(LatLon(lat = 2.0, lon = 2.0), result.unmatchedPoints[0])
    }
}
