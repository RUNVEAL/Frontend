package com.sloth.runveal.dong.glow

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class DongGlowPolylabelTest {

    @Test
    fun squareInradiusApproximatesHalfSideLength() {
        // 한 변 2a = 20인 정사각형 -> 내접원 반지름 ≈ a = 10
        val a = 10.0
        val square = listOf(
            Vec2(-a, -a), Vec2(a, -a), Vec2(a, a), Vec2(-a, a),
        )
        val result = DongGlowPolylabel.pole(square, precisionMeters = 0.1)

        assertTrue(abs(result.distance - a) < 0.2, "distance=${result.distance}, expected ~$a")
        assertTrue(abs(result.point.x) < 0.5 && abs(result.point.y) < 0.5, "point=${result.point}, expected near origin")
    }

    @Test
    fun holeNearCenterReducesDistanceFromPlainSquare() {
        val a = 10.0
        val square = listOf(
            Vec2(-a, -a), Vec2(a, -a), Vec2(a, a), Vec2(-a, a),
        )
        val holeHalf = 3.0
        val hole = listOf(
            Vec2(-holeHalf, -holeHalf), Vec2(holeHalf, -holeHalf), Vec2(holeHalf, holeHalf), Vec2(-holeHalf, holeHalf),
        )

        val withoutHole = DongGlowPolylabel.pole(square, precisionMeters = 0.1)
        val withHole = DongGlowPolylabel.pole(square, holes = listOf(hole), precisionMeters = 0.1)

        assertTrue(withHole.distance > 0.0, "withHole.distance should stay positive (inside the donut)")
        assertTrue(withHole.distance < withoutHole.distance, "hole should shrink the usable inradius")
    }
}
