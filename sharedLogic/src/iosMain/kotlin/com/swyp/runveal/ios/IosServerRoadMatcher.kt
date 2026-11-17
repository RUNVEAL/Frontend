package com.swyp.runveal.ios

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.match.MatchResult
import com.swyp.runveal.match.RoadMatcher
import com.swyp.runveal.network.match.MatchApi
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.match.MatchRequest

/**
 * 실서버 `POST /api/v1/match`(`docs/api/API.md` §3.6/§3.7)로 도로매칭한다 —
 * Android `core:tracking`의 `ServerRoadMatcher`와 같은 구현.
 *
 * `ChunkedMatcher`가 2,000점 단위로 쪼개서 호출하므로 `/meta`의 `limits.match.maxPoints` 제한도
 * 그대로 지켜진다.
 *
 * TODO: Android 쪽 같은 클래스가 `core:tracking`(Android 전용 모듈)에 있어서 두 벌이 됐다.
 *  commonMain으로 올리면 한 벌로 합칠 수 있다 — 의존하는 건 전부 commonMain 타입뿐이다.
 */
internal class IosServerRoadMatcher(
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
