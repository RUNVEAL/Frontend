package com.sloth.runveal.match

import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon

/**
 * 서버 도로 매칭 경로(`matchedPath`)에서 **실제로 달린 자리와 동떨어진 구간을 잘라낸다.**
 *
 * 지도 매칭은 연속된 점 사이를 도로망을 따라 이어서 돌려준다. 그래서 도로 데이터가 없는 곳(산길·공원·
 * 신설 도로)에 들어가면, 마지막으로 도로에 붙었던 지점과 다시 붙는 지점 사이를 **도로 경로로 메워서**
 * 준다 — 가지도 않은 길이 "오늘 달린 길"로 그려지고, 러닝이 끝나면 `track_edge`에 그대로 저장된다.
 *
 * 그래서 매칭 경로의 점마다 원본 GPS 점과의 거리를 재서 [maxDeviationMeters]를 넘으면 버리고, **버린
 * 자리는 잇지 않고 선을 나눈다**(이어 버리면 메운 구간을 직선으로 바꿔 그릴 뿐 결과가 같다). 점이
 * 하나만 남은 조각은 그릴 선이 없으니 버린다.
 */
object MatchedPathFilter {
    /**
     * 기본 허용 오차. `/meta`의 `matchDistanceM`(15m, 도로에 붙었다고 보는 거리)에 GPS 오차를 더해
     * 잡은 값이다 — 도로 폭·가로수·건물 반사까지 감안하면 정상 매칭도 20~30m는 벌어진다.
     */
    const val DEFAULT_MAX_DEVIATION_METERS = 40.0

    fun keepNearRecordedPoints(
        matchedPath: List<LatLon>,
        recordedPoints: List<LatLon>,
        maxDeviationMeters: Double = DEFAULT_MAX_DEVIATION_METERS,
    ): List<List<LatLon>> {
        if (matchedPath.size < 2) return emptyList()
        // 원본 GPS가 없으면 비교할 기준이 없다 — 통째로 버리는 대신 그대로 둔다.
        if (recordedPoints.isEmpty()) return listOf(matchedPath)

        val runs = mutableListOf<List<LatLon>>()
        var current = mutableListOf<LatLon>()
        for (point in matchedPath) {
            if (isNearRecorded(point, recordedPoints, maxDeviationMeters)) {
                current.add(point)
            } else if (current.isNotEmpty()) {
                if (current.size >= 2) runs.add(current)
                current = mutableListOf()
            }
        }
        if (current.size >= 2) runs.add(current)
        return runs
    }

    private fun isNearRecorded(point: LatLon, recordedPoints: List<LatLon>, maxDeviationMeters: Double): Boolean =
        recordedPoints.any { GeoMath.distanceMeters(point, it) <= maxDeviationMeters }
}
