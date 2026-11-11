package com.swyp.runveal.tracking

import com.swyp.runveal.dong.MatchedEdge
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
 * 서버 응답(`MatchResponse`)은 이어붙은 경로 하나(`matchedPath`)와 구간 ID 목록(`segmentIds`)만 준다 —
 * 어느 좌표 구간이 어느 segmentId에 속하는지는 안 알려줘서 쪼갤 수가 없다. 그래서 [MatchResult.edges]는
 * `wayId = null`인 edge 하나로 통째로 만든다 — 동별 완주율 계산([com.swyp.runveal.dong.DongCoverageCalculator])은
 * edge 안의 좌표 시퀀스로 동을 다시 찾아내는 방식이라 edge를 안 쪼개도 그대로 동작한다. 서버가 이미
 * 계산해 준 `dongBySegment`/`sigunguCodes`는 지금은 안 쓴다(§3.6 TODO — 로컬 재계산을 대체할 다음 단계).
 *
 * 실서버는 confidence score를 안 돌려주고, 서비스 지역 밖 점은 에러 없이 조용히 빼고 계산하므로
 * ([docs/api/API.md] §7.2) [MatchResult.confidenceScore]는 항상 1.0, `unmatchedPoints`는 항상
 * 빈 목록이다.
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
        val path = response.matchedPath.map { LatLon(lat = it[1], lon = it[0]) }
        return MatchResult(
            path = path,
            edges = listOf(MatchedEdge(wayId = null, coordinates = path)),
            unmatchedPoints = emptyList(),
            confidenceScore = 1.0,
        )
    }
}
