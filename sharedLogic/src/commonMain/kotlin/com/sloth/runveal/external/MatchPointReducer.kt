package com.sloth.runveal.external

/**
 * `/api/v1/match`에 보낼 점을 `docs/api/API.md` §5.1 규칙에 맞게 줄인다 — 앱이 **양끝을 자르고
 * 10초 간격으로 줄인** 10~2,000개를 보내야 한다.
 *
 * 외부 기록은 앱이 직접 기록한 러닝과 달리 간격을 통제할 수 없다(1초 간격으로 수천 점이 오거나,
 * 반대로 몇 분에 한 점만 오기도 한다). 그래서 가져오기 경로에서만 이 축약을 거친다.
 *
 * 양끝을 자르는 이유는 출발·도착 지점에서 GPS가 제자리를 맴돌며 엉뚱한 도로에 붙는 걸 막기
 * 위해서다. 다만 자르고 나면 [MIN_POINTS]를 못 채우는 짧은 기록이 생기므로, 그때는 자르지 않은
 * 원본으로 되돌린다 — 조금 부정확해도 아예 못 가져오는 것보다 낫다.
 */
object MatchPointReducer {
    /** 서버 최소 점 개수(§3.6.1). 이보다 적으면 매칭 요청 자체가 거절된다. */
    const val MIN_POINTS = 10

    /** 서버 최대 점 개수(§3.6.1). 넘으면 ChunkedMatcher가 쪼개지만, 축약으로 대부분 한 번에 들어간다. */
    const val MAX_POINTS = 2000

    private const val SAMPLE_INTERVAL_MS = 10_000L

    /** 양끝에서 각각 잘라낼 시간. 출발 전·도착 후 제자리 GPS를 덜어낸다. */
    private const val TRIM_EDGE_MS = 15_000L

    /**
     * @return 매칭에 보낼 점. [MIN_POINTS]를 못 채우면 빈 목록 — 호출부는 그 기록을 건너뛴다.
     */
    fun reduce(route: List<ExternalWorkoutPoint>): List<ExternalWorkoutPoint> {
        if (route.size < MIN_POINTS) return emptyList()

        val sorted = route.sortedBy { it.timeEpochMs }
        val trimmed = trimEdges(sorted)

        // 가장 많이 줄인 형태부터 보고, 서버 최소 개수를 채우는 첫 번째를 쓴다. 10초 간격까지
        // 줄이면 90초 미만 기록은 10점을 못 채우는데(10초 × 10점), 그렇다고 버리면 짧은 러닝을
        // 통째로 못 가져온다 — 그런 기록은 덜 줄인 형태로 보낸다. 어차피 상한은 2,000점이라
        // 짧은 기록을 촘촘히 보내도 문제가 없다.
        return listOf(sampleEvery(trimmed), sampleEvery(sorted), trimmed, sorted)
            .firstOrNull { it.size >= MIN_POINTS }
            ?.let { capToMax(it) }
            ?: emptyList()
    }

    private fun trimEdges(sorted: List<ExternalWorkoutPoint>): List<ExternalWorkoutPoint> {
        val first = sorted.first().timeEpochMs
        val last = sorted.last().timeEpochMs
        if (last - first <= TRIM_EDGE_MS * 3) return sorted // 너무 짧으면 자를 게 없다.
        return sorted.filter { it.timeEpochMs - first >= TRIM_EDGE_MS && last - it.timeEpochMs >= TRIM_EDGE_MS }
    }

    /** 마지막 채택 점에서 [SAMPLE_INTERVAL_MS] 이상 지난 점만 남긴다. 마지막 점은 항상 포함한다. */
    private fun sampleEvery(sorted: List<ExternalWorkoutPoint>): List<ExternalWorkoutPoint> {
        if (sorted.isEmpty()) return emptyList()
        val kept = mutableListOf(sorted.first())
        sorted.drop(1).forEach { point ->
            if (point.timeEpochMs - kept.last().timeEpochMs >= SAMPLE_INTERVAL_MS) kept += point
        }
        val last = sorted.last()
        if (kept.last() !== last) kept += last
        return kept
    }

    /** 10초 간격으로도 상한을 넘으면(아주 긴 기록) 일정 간격으로 더 걸러낸다. */
    private fun capToMax(points: List<ExternalWorkoutPoint>): List<ExternalWorkoutPoint> {
        if (points.size <= MAX_POINTS) return points
        val step = points.size.toDouble() / MAX_POINTS
        return (0 until MAX_POINTS).map { index -> points[(index * step).toInt()] }
    }
}
