package com.swyp.runveal.match

import com.swyp.runveal.map.LatLon

private const val CHUNK_SIZE = 2000
private const val CHUNK_OVERLAP = 20

/** 서버 `/api/v1/match`의 점 개수 제한(`/meta`의 `limits.match.maxPoints`, 2,000)을 피해
 * [CHUNK_SIZE]점 단위로 나누고, 경계에서 매칭이 끊기지 않게 [CHUNK_OVERLAP]점씩 겹쳐 보낸다. */
object ChunkedMatcher {
    suspend fun matchInChunks(matcher: RoadMatcher, points: List<LatLon>): MatchResult {
        if (points.size <= CHUNK_SIZE) return matcher.match(points)

        val results = buildChunks(points).map { chunk -> matcher.match(chunk) }
        return MatchResult(
            path = results.flatMap { it.path },
            segmentIds = results.flatMap { it.segmentIds }.distinct(),
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
}
