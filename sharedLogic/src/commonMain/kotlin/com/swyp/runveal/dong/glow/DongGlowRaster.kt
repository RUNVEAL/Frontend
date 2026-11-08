package com.swyp.runveal.dong.glow

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 픽셀 좌표(이미지 좌상단 원점, y 아래 방향) 위의 한 점. */
internal data class PxPoint(val x: Double, val y: Double)

/** 픽셀 좌표로 옮긴 폴리곤 한 조각(exterior + holes). [com.swyp.runveal.dong.DongPolygonPart]의 픽셀 버전. */
internal data class PixelPolygonPart(val exterior: List<PxPoint>, val holes: List<List<PxPoint>>)

private data class Edge(val ax: Double, val ay: Double, val bx: Double, val by: Double)

/**
 * 마스크 M(동 안쪽 덮임 비율)과 테두리 S(덮임 비율)를 스캔라인으로 그린다(문서 3-2-4, 3-2-5).
 * 모든 조각의 exterior+hole ring을 한 edge 목록으로 모아 even-odd 규칙을 적용하면 구멍과
 * MultiPolygon(여러 조각)이 자연스럽게 함께 처리된다.
 */
internal object DongGlowRaster {
    private const val SUBROWS = 4

    /** 세로 [SUBROWS]줄 평균 × 가로는 각 스팬과 픽셀의 정확한 겹침 길이로 안티에일리어싱한다. */
    fun rasterizeMask(parts: List<PixelPolygonPart>, width: Int, height: Int): FloatArray {
        val edges = collectEdges(parts)
        val mask = FloatArray(width * height)
        val rowAcc = FloatArray(width)
        for (y in 0 until height) {
            rowAcc.fill(0f)
            for (sub in 0 until SUBROWS) {
                val sy = y + (sub + 0.5) / SUBROWS
                val xs = intersections(edges, sy)
                var i = 0
                while (i + 1 < xs.size) {
                    addSpanCoverage(rowAcc, xs[i], xs[i + 1], width)
                    i += 2
                }
            }
            val rowOffset = y * width
            for (x in 0 until width) mask[rowOffset + x] = (rowAcc[x] / SUBROWS).coerceIn(0f, 1f)
        }
        return mask
    }

    /**
     * [strokeWidthPx]는 이미 `strokeWidth(level) * effScale`로 맞춰진 값. 각 변의 bbox(두께만큼
     * 부풀린 범위) 안에서만 거리 계산을 해서, 전체 픽셀 × 변 개수를 다 돌지 않는다. 변마다 "가장
     * 가까운 변까지의 거리" 캡슐 모양이 나와서 이음도 자연히 둥글다.
     */
    fun rasterizeStroke(parts: List<PixelPolygonPart>, width: Int, height: Int, strokeWidthPx: Float): FloatArray {
        val stroke = FloatArray(width * height)
        if (strokeWidthPx <= 0f) return stroke
        val halfWidth = strokeWidthPx / 2.0
        val reach = halfWidth + 1.0
        for (edge in collectEdges(parts)) {
            val minXe = min(edge.ax, edge.bx) - reach
            val maxXe = max(edge.ax, edge.bx) + reach
            val minYe = min(edge.ay, edge.by) - reach
            val maxYe = max(edge.ay, edge.by) + reach
            val xStart = max(0, floor(minXe).toInt())
            val xEnd = min(width - 1, ceil(maxXe).toInt())
            val yStart = max(0, floor(minYe).toInt())
            val yEnd = min(height - 1, ceil(maxYe).toInt())
            if (xStart > xEnd || yStart > yEnd) continue
            for (py in yStart..yEnd) {
                val rowOffset = py * width
                for (px in xStart..xEnd) {
                    val d = pointSegmentDistance(px + 0.5, py + 0.5, edge)
                    val coverage = (halfWidth - d + 0.5).coerceIn(0.0, 1.0)
                    val idx = rowOffset + px
                    if (coverage > stroke[idx]) stroke[idx] = coverage.toFloat()
                }
            }
        }
        return stroke
    }

    private fun addSpanCoverage(rowAcc: FloatArray, spanStartX: Double, spanEndX: Double, width: Int) {
        if (spanEndX <= spanStartX) return
        val s = spanStartX.coerceIn(0.0, width.toDouble())
        val e = spanEndX.coerceIn(0.0, width.toDouble())
        if (e <= s) return
        var xi = floor(s).toInt()
        val xiEnd = min(ceil(e).toInt(), width)
        while (xi < xiEnd) {
            val overlap = (min(e, xi + 1.0) - max(s, xi.toDouble())).coerceAtLeast(0.0)
            rowAcc[xi] += overlap.toFloat()
            xi++
        }
    }

    private fun intersections(edges: List<Edge>, y: Double): List<Double> {
        val xs = mutableListOf<Double>()
        for (edge in edges) {
            if ((edge.ay > y) != (edge.by > y)) {
                val t = (y - edge.ay) / (edge.by - edge.ay)
                xs += edge.ax + t * (edge.bx - edge.ax)
            }
        }
        xs.sort()
        return xs
    }

    private fun pointSegmentDistance(px: Double, py: Double, edge: Edge): Double {
        var x = edge.ax
        var y = edge.ay
        val dx = edge.bx - x
        val dy = edge.by - y
        if (dx != 0.0 || dy != 0.0) {
            val t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy)
            if (t > 1) {
                x = edge.bx; y = edge.by
            } else if (t > 0) {
                x += dx * t; y += dy * t
            }
        }
        val ddx = px - x
        val ddy = py - y
        return kotlin.math.sqrt(ddx * ddx + ddy * ddy)
    }

    private fun collectEdges(parts: List<PixelPolygonPart>): List<Edge> {
        val edges = mutableListOf<Edge>()
        for (part in parts) {
            addRingEdges(edges, part.exterior)
            for (hole in part.holes) addRingEdges(edges, hole)
        }
        return edges
    }

    private fun addRingEdges(edges: MutableList<Edge>, ring: List<PxPoint>) {
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            edges += Edge(a.x, a.y, b.x, b.y)
        }
    }
}
