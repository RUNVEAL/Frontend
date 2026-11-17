package com.sloth.runveal.dong

import com.sloth.runveal.map.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class PolylabelTest {

    @Test
    fun squareFindsPointNearCenter() {
        val square = listOf(
            LatLon(36.70, 127.00), LatLon(36.70, 127.10),
            LatLon(36.80, 127.10), LatLon(36.80, 127.00), LatLon(36.70, 127.00),
        )
        val pole = Polylabel.pole(square)
        assertTrue(abs(pole.lat - 36.75) < 0.01)
        assertTrue(abs(pole.lon - 127.05) < 0.01)
    }

    @Test
    fun concaveLShapeStaysInsidePolygon() {
        // L자: 오른쪽 위 사분면이 파인 모양. 단순 꼭짓점 평균(centroid)은 파인 부분(바깥)에 찍힌다.
        val lShape = listOf(
            LatLon(36.70, 127.00),
            LatLon(36.70, 127.20),
            LatLon(36.75, 127.20),
            LatLon(36.75, 127.10),
            LatLon(36.90, 127.10),
            LatLon(36.90, 127.00),
            LatLon(36.70, 127.00),
        )
        val pole = Polylabel.pole(lShape, precisionMeters = 5.0)
        assertTrue(pole.lon < 127.10, "극점이 L자 안쪽(세로 막대)에 있어야 한다")
        assertTrue(pole.lat in 36.70..36.90)
    }

    @Test
    fun holeInPolygonPushesPoleOutsideHole() {
        val outer = listOf(
            LatLon(36.70, 127.00), LatLon(36.70, 127.10),
            LatLon(36.80, 127.10), LatLon(36.80, 127.00), LatLon(36.70, 127.00),
        )
        // 바깥 사각형 중심을 거의 꽉 채우는 구멍 — 극점은 구멍을 피해 가장자리 쪽으로 밀려나야 한다.
        val hole = listOf(
            LatLon(36.705, 127.005), LatLon(36.705, 127.095),
            LatLon(36.795, 127.095), LatLon(36.795, 127.005), LatLon(36.705, 127.005),
        )
        val pole = Polylabel.pole(outer, holes = listOf(hole), precisionMeters = 2.0)
        val insideHole = pole.lat in 36.705..36.795 && pole.lon in 127.005..127.095
        assertTrue(!insideHole, "극점이 구멍 안에 있으면 안 된다")
        val insideOuter = pole.lat in 36.70..36.80 && pole.lon in 127.00..127.10
        assertTrue(insideOuter, "극점이 바깥 폴리곤 안에는 있어야 한다")
    }
}
