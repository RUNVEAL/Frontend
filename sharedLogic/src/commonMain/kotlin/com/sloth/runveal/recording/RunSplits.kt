package com.sloth.runveal.recording

import com.sloth.runveal.map.GeoMath

/** 구간 기록 계산에 쓰는 경로 한 점 — GPX의 `<trkpt>` 하나에 해당한다. */
data class SplitPoint(val lat: Double, val lon: Double, val timeEpochMs: Long)

/** 1km 구간 하나. [index]는 1부터(1km, 2km…), [paceSecondsPerKm]는 그 구간만의 페이스다. */
data class RunSplit(val index: Int, val durationMs: Long, val paceSecondsPerKm: Double)

/**
 * 기록의 경로 점을 1km마다 끊어 구간별 페이스를 낸다(기록 상세 "구간 기록").
 *
 * 경계를 **점 사이에서** 넘어가므로 넘어간 시각은 두 점 사이를 거리 비율로 나눠 추정한다 — 안 그러면
 * 점 간격(보통 1초, 달리면 3~5m)만큼 구간 경계가 밀려 페이스가 들쭉날쭉해진다.
 *
 * 마지막 1km를 못 채운 자투리는 버린다 — 0.2km만 달린 구간의 페이스를 1km 구간들과 나란히 두면
 * 비교가 안 된다(시안도 채운 구간만 보여준다).
 */
object RunSplits {
    const val DEFAULT_SPLIT_METERS = 1000.0

    fun of(points: List<SplitPoint>, splitMeters: Double = DEFAULT_SPLIT_METERS): List<RunSplit> {
        if (points.size < 2 || splitMeters <= 0.0) return emptyList()

        val splits = mutableListOf<RunSplit>()
        var splitStartTimeMs = points.first().timeEpochMs
        // 지금 구간에서 아직 못 채운 거리. 0 이하로 떨어지는 순간이 경계다.
        var remainingMeters = splitMeters

        points.zipWithNext().forEach { (from, to) ->
            val segmentMeters = GeoMath.distanceMeters(from.lat, from.lon, to.lat, to.lon)
            if (segmentMeters <= 0.0) return@forEach
            val segmentDurationMs = to.timeEpochMs - from.timeEpochMs
            // 점 간격이 길면 한 번에 여러 경계를 넘을 수 있다 — 넘을 때마다 시각을 다시 잡는다.
            var leftoverMeters = segmentMeters
            // 이 점 사이에서 지금까지 지나온 비율(0~1) — 경계 시각을 거리 비율로 추정하는 데 쓴다.
            var consumedRatio = 0.0

            while (leftoverMeters >= remainingMeters) {
                consumedRatio += remainingMeters / segmentMeters
                val crossedAtMs = from.timeEpochMs + (segmentDurationMs * consumedRatio).toLong()
                val durationMs = crossedAtMs - splitStartTimeMs
                splits += RunSplit(
                    index = splits.size + 1,
                    durationMs = durationMs,
                    paceSecondsPerKm = durationMs / 1000.0 / (splitMeters / 1000.0),
                )
                splitStartTimeMs = crossedAtMs
                leftoverMeters -= remainingMeters
                remainingMeters = splitMeters
            }
            remainingMeters -= leftoverMeters
        }
        return splits
    }
}
