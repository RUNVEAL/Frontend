package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.map.LatLon
import kotlin.math.floor
import kotlin.math.sqrt

private const val METERS_PER_DEGREE_LAT = 111_320.0
private const val METERS_PER_DEGREE_LON = 88_316.0 // 위도 37.5도(대략 한국) 기준 근사치

/**
 * 서버 index.geojson은 동마다 독립적으로 단순화돼서, 원래 정확히 같았던 인접 동의 공유 경계 정점이
 * 동마다 몇 m씩 어긋나 있다 — 경계선이 색 다른 두 줄로 겹쳐 보이는 원인. 전체 동을 순회하며 톨러런스
 * 안의 가까운 정점을 먼저 본 점으로 합쳐서, 공유 경계가 다시 정확히 같은 좌표를 쓰게 만든다.
 */
internal class VertexSnapper(private val toleranceMeters: Double = 15.0) {
    private val cellSizeDeg = toleranceMeters / METERS_PER_DEGREE_LAT
    private val grid = HashMap<Pair<Long, Long>, MutableList<LatLon>>()

    fun snap(point: LatLon): LatLon {
        val cx = cellIndex(point.lon)
        val cy = cellIndex(point.lat)
        val canonical = nearestWithinTolerance(point, cx, cy) ?: point
        grid.getOrPut(cx to cy) { mutableListOf() }.add(canonical)
        return canonical
    }

    private fun nearestWithinTolerance(point: LatLon, cx: Long, cy: Long): LatLon? {
        var nearest: LatLon? = null
        var nearestDist = toleranceMeters
        for (dx in -1..1) {
            for (dy in -1..1) {
                grid[(cx + dx) to (cy + dy)]?.forEach { candidate ->
                    val dist = distanceMeters(point, candidate)
                    if (dist < nearestDist) {
                        nearest = candidate
                        nearestDist = dist
                    }
                }
            }
        }
        return nearest
    }

    private fun distanceMeters(a: LatLon, b: LatLon): Double {
        val dLon = (a.lon - b.lon) * METERS_PER_DEGREE_LON
        val dLat = (a.lat - b.lat) * METERS_PER_DEGREE_LAT
        return sqrt(dLon * dLon + dLat * dLat)
    }

    private fun cellIndex(value: Double): Long = floor(value / cellSizeDeg).toLong()
}
