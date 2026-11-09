package com.swyp.runveal.dong.glow

import com.swyp.runveal.map.LatLon
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

/** Mercator 평면(미터) 위의 한 점. */
internal data class Vec2(val x: Double, val y: Double)

/**
 * Web Mercator 투영(구형 지구, R = 6378137). `tools/dong-glow/figma_fx.py`와 같은 공식 —
 * 동 빛 이미지는 피그마 시안과 픽셀 단위로 맞춘 결과라, 다른 투영(예: 접평면 근사)을 쓰면 특히 큰 동에서
 * 오차가 누적된다.
 */
internal object Mercator {
    const val EARTH_RADIUS_M = 6378137.0

    fun project(point: LatLon): Vec2 {
        val lambda = toRadians(point.lon)
        val phi = toRadians(point.lat)
        val x = EARTH_RADIUS_M * lambda
        val y = EARTH_RADIUS_M * ln(tan(PI / 4.0 + phi / 2.0))
        return Vec2(x, y)
    }

    fun unproject(point: Vec2): LatLon {
        val lambda = point.x / EARTH_RADIUS_M
        val phi = 2.0 * atan(exp(point.y / EARTH_RADIUS_M)) - PI / 2.0
        return LatLon(lat = toDegrees(phi), lon = toDegrees(lambda))
    }

    private fun toRadians(degrees: Double): Double = degrees / 180.0 * PI
    private fun toDegrees(radians: Double): Double = radians / PI * 180.0
}
