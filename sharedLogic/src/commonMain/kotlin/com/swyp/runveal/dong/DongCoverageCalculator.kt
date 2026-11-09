package com.swyp.runveal.dong

import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon

/**
 * 동별 가본 도로 길이(분자) 계산. 매칭 edge를 두 점씩 구간으로 쪼개 구간 중간점이 속한 동에 길이를
 * 더한다 — 분모(동 전체 도로 길이)를 구하는 방식과 같은 "중간점 기준"이라 분모·분자가 서로 맞는다.
 * wayId가 같은 edge는 여러 기록(트랙)에 걸쳐 나와도 한 번만 센다(같은 길을 여러 번 걸은 경우).
 */
object DongCoverageCalculator {

    fun walkedLengthByDong(boundaries: List<DongBoundary>, edges: List<MatchedEdge>): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        for (edge in dedupByWayId(edges)) {
            val points = edge.coordinates
            for (i in 0 until points.size - 1) {
                val a = points[i]
                val b = points[i + 1]
                val midpoint = LatLon(lat = (a.lat + b.lat) / 2, lon = (a.lon + b.lon) / 2)
                val admCd2 = findDong(boundaries, midpoint) ?: continue
                totals[admCd2] = (totals[admCd2] ?: 0.0) + GeoMath.distanceMeters(a, b)
            }
        }
        return totals
    }

    /** 주어진 점이 속한 동의 adm_cd2. 어느 동에도 속하지 않으면 null(경계 밖/바다 등). */
    fun findDong(boundaries: List<DongBoundary>, point: LatLon): String? =
        boundaries.firstOrNull { boundary -> boundary.rings.any { ring -> pointInRing(point, ring) } }?.admCd2

    private fun dedupByWayId(edges: List<MatchedEdge>): List<MatchedEdge> {
        val seenWayIds = mutableSetOf<Long>()
        return edges.filter { edge ->
            val wayId = edge.wayId ?: return@filter true
            seenWayIds.add(wayId)
        }
    }

    /** 표준 ray casting point-in-polygon. lon을 x, lat을 y로 쓴다(한 시/군 규모라 투영 없이 충분히 정확). */
    private fun pointInRing(point: LatLon, ring: List<LatLon>): Boolean {
        var inside = false
        val n = ring.size
        var j = n - 1
        for (i in 0 until n) {
            val xi = ring[i].lon
            val yi = ring[i].lat
            val xj = ring[j].lon
            val yj = ring[j].lat
            if ((yi > point.lat) != (yj > point.lat) &&
                point.lon < (xj - xi) * (point.lat - yi) / (yj - yi + 1e-15) + xi
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
