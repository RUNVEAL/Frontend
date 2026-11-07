package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon

/**
 * RFC 7946(GeoJSON) 기준으로 폴리곤 ring의 감긴 방향을 다룬다: 외곽 ring은 반시계(CCW), 구멍 ring은
 * 시계(CW)여야 한다. 데이터가 이미 이 규칙을 따른다는 보장이 없어서(소스마다 다를 수 있음), MapLibre의
 * `line-offset`으로 안쪽 글로우를 그리기 전에 방향을 맞춰 둬야 한다 — line-offset은 "진행 방향 기준
 * 오른쪽이 양수"라서, 링이 거꾸로 감겨 있으면 글로우가 바깥으로 샌다.
 */
object RingWinding {

    /** lon을 x, lat을 y로 둔 신 발끈(shoelace) 부호 있는 면적. 양수면 반시계(CCW), 음수면 시계(CW). */
    fun signedArea(ring: List<LatLon>): Double {
        if (ring.size < 3) return 0.0
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a.lon * b.lat - b.lon * a.lat
        }
        return sum / 2.0
    }

    fun isCounterClockwise(ring: List<LatLon>): Boolean = signedArea(ring) > 0.0

    /** 외곽 ring을 CCW로, 구멍 ring들을 CW로 — 이미 맞으면 그대로, 아니면 뒤집는다. */
    fun normalize(exterior: List<LatLon>, holes: List<List<LatLon>>): DongPolygonPart {
        val fixedExterior = if (isCounterClockwise(exterior)) exterior else exterior.asReversed()
        val fixedHoles = holes.map { hole -> if (isCounterClockwise(hole)) hole.asReversed() else hole }
        return DongPolygonPart(exterior = fixedExterior, holes = fixedHoles)
    }
}
