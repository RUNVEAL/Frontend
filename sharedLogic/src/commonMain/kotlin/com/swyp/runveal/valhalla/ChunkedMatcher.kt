package com.swyp.runveal.valhalla

import com.swyp.runveal.dong.MatchedEdge
import com.swyp.runveal.map.LatLon
import kotlin.math.round

private const val CHUNK_SIZE = 2000
private const val CHUNK_OVERLAP = 20

/** `feature-valhalla-matching.md` "긴 기록 쪼개기: ChunkedMatcher" — Valhalla 점 개수 제한을 피해
 * [CHUNK_SIZE]점 단위로 나누고, 경계에서 매칭이 끊기지 않게 [CHUNK_OVERLAP]점씩 겹쳐 보낸다. */
object ChunkedMatcher {
    suspend fun matchInChunks(matcher: RoadMatcher, points: List<LatLon>): MatchResult {
        if (points.size <= CHUNK_SIZE) return matcher.match(points)

        val results = buildChunks(points).map { chunk -> matcher.match(chunk) }
        return MatchResult(
            path = results.flatMap { it.path },
            edges = dedupEdges(results.flatMap { it.edges }),
            unmatchedPoints = results.flatMap { it.unmatchedPoints },
            confidenceScore = results.map { it.confidenceScore }.average(),
        )
    }

    private fun buildChunks(points: List<LatLon>): List<List<LatLon>> {
        val chunks = mutableListOf<List<LatLon>>()
        var start = 0
        while (start < points.size) {
            val end = minOf(start + CHUNK_SIZE, points.size)
            chunks.add(points.subList(start, end))
            if (end == points.size) break
            start = end - CHUNK_OVERLAP
        }
        return chunks
    }

    /** 겹친 구간에서 같은 edge가 두 조각에 걸쳐 나오는 것을 `wayId` + 양 끝 좌표(소수 5자리)로 걸러낸다. */
    private fun dedupEdges(edges: List<MatchedEdge>): List<MatchedEdge> {
        val seen = mutableSetOf<String>()
        return edges.filter { edge -> seen.add(edgeKey(edge)) }
    }

    private fun edgeKey(edge: MatchedEdge): String {
        val first = edge.coordinates.firstOrNull()?.round5()
        val last = edge.coordinates.lastOrNull()?.round5()
        return "${edge.wayId}:$first:$last"
    }

    private fun LatLon.round5(): String {
        val roundedLat = round(lat * 1e5) / 1e5
        val roundedLon = round(lon * 1e5) / 1e5
        return "$roundedLat,$roundedLon"
    }
}
