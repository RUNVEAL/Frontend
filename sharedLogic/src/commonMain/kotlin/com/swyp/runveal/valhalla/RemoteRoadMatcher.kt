package com.swyp.runveal.valhalla

import com.swyp.runveal.map.LatLon

/** `feature-valhalla-matching.md` "RemoteRoadMatcher" — 입력 좌표를 그대로 요청에 담아 보내고, 응답은
 * 공유 매퍼([TraceResponseMapper])로 변환한다. */
class RemoteRoadMatcher(private val client: ValhallaClient = ValhallaClient()) : RoadMatcher {
    override suspend fun match(points: List<LatLon>): MatchResult {
        val request = TraceRequest(shape = points.map { TracePointDto(lat = it.lat, lon = it.lon) })
        val response = client.traceAttributes(request)
        return TraceResponseMapper.toMatchResult(response)
    }
}
