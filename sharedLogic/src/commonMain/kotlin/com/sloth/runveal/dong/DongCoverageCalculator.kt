package com.sloth.runveal.dong

import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon

/** 좌표가 속한 동을 찾고, 좌표 목록을 동별 길이로 쪼개는 순수 기하 유틸. 구간의 "가본 길" 인정
 * 여부·중복 제거는 이제 segmentId 기준([com.sloth.runveal.dong.RoadSegment]/`explored_segment`)으로
 * 따로 관리해서 여기서는 다루지 않는다. */
object DongCoverageCalculator {

    /** [coordinates]를 두 점씩 구간으로 쪼개 구간 중간점이 속한 동에 길이를 더한다 — 분모(동 전체
     * 도로 길이)를 구하는 방식과 같은 "중간점 기준"이라 분모·분자가 서로 맞는다. */
    fun lengthByDong(boundaries: List<DongBoundary>, coordinates: List<LatLon>): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        for (i in 0 until coordinates.size - 1) {
            val a = coordinates[i]
            val b = coordinates[i + 1]
            val midpoint = LatLon(lat = (a.lat + b.lat) / 2, lon = (a.lon + b.lon) / 2)
            val admCd2 = findDong(boundaries, midpoint) ?: continue
            totals[admCd2] = (totals[admCd2] ?: 0.0) + GeoMath.distanceMeters(a, b)
        }
        return totals
    }

    /** 주어진 점이 속한 동의 adm_cd2. 어느 동에도 속하지 않으면 null(경계 밖/바다 등). */
    fun findDong(boundaries: List<DongBoundary>, point: LatLon): String? =
        boundaries.firstOrNull { boundary -> boundary.rings.any { ring -> pointInRing(point, ring) } }?.admCd2

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
