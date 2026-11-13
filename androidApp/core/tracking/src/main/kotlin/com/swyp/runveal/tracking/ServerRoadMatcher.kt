package com.swyp.runveal.tracking

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.match.MatchResult
import com.swyp.runveal.match.RoadMatcher
import com.swyp.runveal.network.match.MatchApi
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.match.MatchRequest

/**
 * 실서버 `POST /api/v1/match`(`docs/api/API.md` §3.6/§3.7)로 도로매칭한다. [ChunkedMatcher]가 그대로
 * 2,000점 단위로 쪼개서 호출하므로 `/meta`의 `limits.match.maxPoints` 제한도 그대로 지켜진다.
 *
 * 서버가 주는 `segmentIds`를 그대로 [MatchResult.segmentIds]로 넘긴다 — "가본 길" 인정은 이제 이
 * ID 기준([com.swyp.runveal.dong.RoadSegment]/`explored_segment`)이라 로컬에서 별도로 edge를
 * 만들어 둘 필요가 없다. `dongBySegment`/`sigunguCodes`는 로컬 `road_segment` 인덱스로 직접
 * 계산하므로 지금은 안 쓴다.
 */
class ServerRoadMatcher(
    private val mapVersion: String,
    private val purpose: MatchPurpose,
    private val matchApi: MatchApi = MatchApi(),
) : RoadMatcher {
    override suspend fun match(points: List<LatLon>): MatchResult {
        val response = matchApi.match(
            MatchRequest(
                mapVersion = mapVersion,
                purpose = purpose,
                points = points.map { listOf(it.lon, it.lat) },
            ),
        )
        return MatchResult(
            path = response.matchedPath.map { LatLon(lat = it[1], lon = it[0]) },
            segmentIds = response.segmentIds,
        )
    }
}
