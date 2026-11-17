package com.swyp.runveal.match

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.network.match.MatchApi
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.match.MatchRequest

/**
 * 실서버 `POST /api/v1/match`(`docs/api/API.md` §3.6/§3.7)로 도로매칭한다. 플랫폼 의존성이 없어서
 * 러닝 종료(`core:tracking`, `LIVE`)와 외부 기록 가져오기(`core:health`, `IMPORT`)가 같이 쓴다. [ChunkedMatcher]가 그대로
 * 2,000점 단위로 쪼개서 호출하므로 `/meta`의 `limits.match.maxPoints` 제한도 그대로 지켜진다.
 *
 * 서버가 주는 `segmentIds`를 그대로 [MatchResult.segmentIds]로 넘긴다 — "가본 길" 인정은 이제 이
 * ID 기준([com.swyp.runveal.dong.RoadSegment]/`explored_segment`)이라 로컬에서 별도로 edge를
 * 만들어 둘 필요가 없다. `sigunguCodes`도 같이 넘겨서, 로컬 구간 인덱스를 전국이 아니라 이번에
 * 달린 구만 골라 들여오는 데 쓴다. `dongBySegment`는 그 인덱스로 직접 계산하므로 아직 안 쓴다.
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
            sigunguCodes = response.sigunguCodes,
        )
    }
}
