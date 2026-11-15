package com.swyp.runveal.dong

import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import kotlin.math.max

/**
 * 서버 구간([RoadSegment])이 주어진 경로([referencePaths] — 예: 러닝 중 GPS 경로)로 얼마나 덮였는지
 * 로컬 판정한다. `docs/api/API.md` §3.1.2의 두 기준 그대로:
 * - `matchDistanceM`: 도로선에서 이 거리(m) 안이면 지나간 것
 * - `segmentCoverage`: 구간의 이 비율 이상 지나가면 인정
 *
 * [SAMPLE_STEP_METERS] 간격으로 구간을 샘플링해서, 각 샘플 점이 [referencePaths] 중 어느 하나의
 * 선분과 [matchDistanceM] 이내인지를 보고, 그 비율로 판정한다.
 */
object SegmentCoverageMatcher {
    /** 구간을 이 간격(m)으로 샘플링한다 — [CoverageMask] 비트 하나가 이 길이를 뜻한다. */
    const val SAMPLE_STEP_METERS = 5.0

    /** `/meta` manifest `segmentCoverage` 기본값(`docs/api/API.md` §3.1.2) — 구간의 이 비율 이상을
     * 지나갔으면 "그 구간은 가봤다"로 친다. */
    const val DEFAULT_SEGMENT_COVERAGE = 0.6

    fun isCovered(
        segment: RoadSegment,
        referencePaths: List<List<LatLon>>,
        matchDistanceM: Double,
        segmentCoverage: Double,
    ): Boolean {
        val samples = samplePoints(segment.lines)
        if (samples.isEmpty()) return false
        return coverageMask(samples, referencePaths, matchDistanceM).coveredRatio >= segmentCoverage
    }

    /** 구간 지오메트리를 [SAMPLE_STEP_METERS] 간격 샘플 점으로 — 조각(MultiLineString)별로 나눠서
     * 돌려준다. 조각 경계를 넘어 선을 잇지 않으려면([coveredSubPaths]) 어디서 끊기는지 알아야 한다. */
    fun samplePointsByLine(lines: List<List<LatLon>>): List<List<LatLon>> = lines.map { sampleLine(it) }

    /** [samplePointsByLine]을 이어 붙인 것 — [CoverageMask] 비트 순서의 기준이다. */
    fun samplePoints(lines: List<List<LatLon>>): List<LatLon> = samplePointsByLine(lines).flatten()

    /**
     * [samples] 중 [referencePaths]에서 [matchDistanceM] 안에 있는 것만 켠 [CoverageMask] — 구간의
     * **어느 부분을** 지나갔는지다. 구간 전체를 통째로 인정하던([isCovered]) 자리를 대신한다.
     */
    fun coverageMask(
        samples: List<LatLon>,
        referencePaths: List<List<LatLon>>,
        matchDistanceM: Double,
    ): CoverageMask {
        val mask = CoverageMask.empty(samples.size)
        samples.forEachIndexed { index, sample ->
            if (isNearAnyPath(sample, referencePaths, matchDistanceM)) mask.set(index)
        }
        return mask
    }

    /** [mask]에서 켜진 샘플이 **연달아 이어지는 토막**들의 선 — 지도에 "실제로 지난 부분"만 덧그릴 때
     * 쓴다. 조각([samplePointsByLine])을 넘어가면 끊는다(떨어진 두 조각을 직선으로 이으면 안 지난 곳에
     * 선이 생긴다). */
    fun coveredSubPaths(sampleLines: List<List<LatLon>>, mask: CoverageMask): List<List<LatLon>> {
        val paths = mutableListOf<List<LatLon>>()
        var offset = 0
        sampleLines.forEach { line ->
            var current = mutableListOf<LatLon>()
            line.forEachIndexed { index, point ->
                if (mask[offset + index]) {
                    current.add(point)
                } else {
                    if (current.size >= 2) paths += current
                    current = mutableListOf()
                }
            }
            if (current.size >= 2) paths += current
            offset += line.size
        }
        return paths
    }

    /** [point]가 [paths] 중 어느 하나의 선분과 [matchDistanceM] 이내인지 — 실시간 분류([LiveRoadClassifier])
     * 등 "이 점이 이 도로 조각 근처냐"만 따로 필요할 때 쓴다. */
    fun isNearAnyPath(point: LatLon, paths: List<List<LatLon>>, matchDistanceM: Double): Boolean =
        paths.any { path -> isNearPath(point, path, matchDistanceM) }

    /** [point]에서 [paths]까지의 최단거리(m). 선이 없으면 [Double.MAX_VALUE].
     *
     * "근처냐"가 아니라 거리가 필요한 곳 — 예를 들어 **여러 구간 중 어느 쪽 위에 서 있는지** 고를 때
     * 쓴다. 나란한 도로·인도가 15m 안에 같이 잡히면 "근처냐"만으로는 구분이 안 된다. */
    fun distanceToPathsMeters(point: LatLon, paths: List<List<LatLon>>): Double =
        paths.minOfOrNull { path -> distanceToPathMeters(point, path) } ?: Double.MAX_VALUE

    private fun distanceToPathMeters(point: LatLon, path: List<LatLon>): Double {
        if (path.isEmpty()) return Double.MAX_VALUE
        if (path.size < 2) return GeoMath.distanceMeters(point, path.first())
        var nearest = Double.MAX_VALUE
        for (i in 0 until path.size - 1) {
            val distance = distanceToSegmentMeters(point, path[i], path[i + 1])
            if (distance < nearest) nearest = distance
        }
        return nearest
    }

    private fun sampleLine(line: List<LatLon>): List<LatLon> {
        if (line.size < 2) return line
        val samples = mutableListOf<LatLon>()
        for (i in 0 until line.size - 1) {
            val a = line[i]
            val b = line[i + 1]
            val steps = max(1, (GeoMath.distanceMeters(a, b) / SAMPLE_STEP_METERS).toInt())
            for (s in 0 until steps) {
                val t = s.toDouble() / steps
                samples += LatLon(lat = a.lat + (b.lat - a.lat) * t, lon = a.lon + (b.lon - a.lon) * t)
            }
        }
        samples += line.last()
        return samples
    }

    private fun isNearPath(point: LatLon, path: List<LatLon>, matchDistanceM: Double): Boolean {
        if (path.size < 2) return path.any { GeoMath.distanceMeters(point, it) <= matchDistanceM }
        for (i in 0 until path.size - 1) {
            if (distanceToSegmentMeters(point, path[i], path[i + 1]) <= matchDistanceM) return true
        }
        return false
    }

    /** 점-선분 최단거리(m) — lon/lat을 평면 좌표처럼 다뤄 근사한다(시/군 규모라 투영 없이 충분히
     * 정확하다는 전제는 [DongCoverageCalculator.pointInRing]과 같다). */
    private fun distanceToSegmentMeters(point: LatLon, a: LatLon, b: LatLon): Double {
        val dx = b.lon - a.lon
        val dy = b.lat - a.lat
        if (dx == 0.0 && dy == 0.0) return GeoMath.distanceMeters(point, a)
        val t = (((point.lon - a.lon) * dx) + ((point.lat - a.lat) * dy)) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0.0, 1.0)
        val closest = LatLon(lat = a.lat + dy * clamped, lon = a.lon + dx * clamped)
        return GeoMath.distanceMeters(point, closest)
    }
}
