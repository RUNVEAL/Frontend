package com.swyp.runveal.valhalla

import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.LatLon

private const val UNMATCHED = "unmatched"

/** `feature-valhalla-matching.md` "변환: TraceResponseMapper" — Remote/Mock이 공유하는 순수 변환 로직. */
object TraceResponseMapper {
    fun toMatchResult(response: TraceResponse): MatchResult {
        val path = response.shape?.let(Polyline6::decode) ?: emptyList()

        val edges = response.edges.mapNotNull { edge -> sliceEdge(path, edge) }

        val unmatchedPoints = response.matchedPoints
            .filter { it.type == UNMATCHED }
            .mapNotNull { point -> if (point.lat != null && point.lon != null) LatLon(lat = point.lat, lon = point.lon) else null }

        return MatchResult(
            path = path,
            edges = edges,
            unmatchedPoints = unmatchedPoints,
            confidenceScore = response.confidenceScore,
        )
    }

    /** 인덱스가 없거나 `path` 범위를 벗어나면 그 edge는 버린다(§변환 2). */
    private fun sliceEdge(path: List<LatLon>, edge: EdgeDto): MatchedEdge? {
        val begin = edge.beginShapeIndex ?: return null
        val end = edge.endShapeIndex ?: return null
        if (begin < 0 || end >= path.size || begin > end) return null
        return MatchedEdge(wayId = edge.wayId, coordinates = path.subList(begin, end + 1))
    }
}
