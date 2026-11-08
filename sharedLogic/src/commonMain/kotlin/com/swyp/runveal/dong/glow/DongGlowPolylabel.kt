package com.swyp.runveal.dong.glow

import kotlin.math.sqrt

/**
 * [com.swyp.runveal.dong.Polylabel]과 같은 "접근 불가능한 극"(pole of inaccessibility) 알고리즘을,
 * 이미 투영된 Mercator 평면(미터) 좌표에서 직접 계산한다. [com.swyp.runveal.dong.Polylabel]은 라벨
 * 위치용으로 위경도를 중심점 접평면에 근사 투영해서 쓰는데, 빛 이미지는 피그마 시안과 픽셀 단위로 맞춘
 * 결과라 정확한 Mercator 좌표가 필요해서 따로 둔다.
 */
internal object DongGlowPolylabel {
    private const val SQRT2 = 1.4142135623730951
    private const val MAX_ITERATIONS = 20000

    data class Result(val point: Vec2, val distance: Double)

    /** [exterior](+[holes])의 극과, 그 점에서 경계(구멍 포함)까지의 거리(미터)를 함께 돌려준다. */
    fun pole(exterior: List<Vec2>, holes: List<List<Vec2>> = emptyList(), precisionMeters: Double): Result {
        require(exterior.size >= 3) { "exterior ring needs at least 3 points" }
        val rings = listOf(exterior) + holes

        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (p in exterior) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        val width = maxX - minX
        val height = maxY - minY

        fun distToRings(x: Double, y: Double): Double {
            var inside = false
            var minDistSq = Double.MAX_VALUE
            for (ring in rings) {
                var j = ring.size - 1
                for (i in ring.indices) {
                    val a = ring[i]
                    val b = ring[j]
                    if ((a.y > y) != (b.y > y) && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x) {
                        inside = !inside
                    }
                    minDistSq = minOf(minDistSq, segDistSq(x, y, a, b))
                    j = i
                }
            }
            val d = sqrt(minDistSq)
            return if (inside) d else -d
        }

        if (width <= 0.0 || height <= 0.0) {
            val cx = (minX + maxX) / 2
            val cy = (minY + maxY) / 2
            return Result(Vec2(cx, cy), distToRings(cx, cy))
        }

        class Cell(val x: Double, val y: Double, val h: Double) {
            val d = distToRings(x, y)
            val max = d + h * SQRT2
        }

        val cellSize = minOf(width, height)
        var h = cellSize / 2
        val queue = GlowMaxHeap<Cell> { it.max }
        var gx = minX
        while (gx < maxX) {
            var gy = minY
            while (gy < maxY) {
                queue.offer(Cell(gx + h, gy + h, h))
                gy += cellSize
            }
            gx += cellSize
        }

        var best = Cell((minX + maxX) / 2, (minY + maxY) / 2, 0.0)
        val centroidX = exterior.sumOf { it.x } / exterior.size
        val centroidY = exterior.sumOf { it.y } / exterior.size
        val centroidCell = Cell(centroidX, centroidY, 0.0)
        if (centroidCell.d > best.d) best = centroidCell

        var guard = 0
        while (!queue.isEmpty() && guard < MAX_ITERATIONS) {
            guard++
            val cell = queue.poll()
            if (cell.d > best.d) best = cell
            if (cell.max - best.d <= precisionMeters) continue
            val half = cell.h / 2
            queue.offer(Cell(cell.x - half, cell.y - half, half))
            queue.offer(Cell(cell.x + half, cell.y - half, half))
            queue.offer(Cell(cell.x - half, cell.y + half, half))
            queue.offer(Cell(cell.x + half, cell.y + half, half))
        }
        return Result(Vec2(best.x, best.y), best.d)
    }

    /** 문서 3-2-2 허용오차: `max(sqrt(조각 넓이) / 200, 0.5)` 미터. */
    fun precisionFor(exterior: List<Vec2>): Double = maxOf(sqrt(ringAreaAbs(exterior)) / 200.0, 0.5)

    fun ringAreaAbs(ring: List<Vec2>): Double {
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a.x * b.y - b.x * a.y
        }
        return kotlin.math.abs(sum) / 2.0
    }

    private fun segDistSq(px: Double, py: Double, a: Vec2, b: Vec2): Double {
        var x = a.x
        var y = a.y
        var dx = b.x - x
        var dy = b.y - y
        if (dx != 0.0 || dy != 0.0) {
            val t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy)
            if (t > 1) {
                x = b.x; y = b.y
            } else if (t > 0) {
                x += dx * t; y += dy * t
            }
        }
        dx = px - x
        dy = py - y
        return dx * dx + dy * dy
    }
}

/** `java.util.PriorityQueue`가 없는 commonMain용 최소 배열 기반 최대 힙. [com.swyp.runveal.dong.Polylabel]의 것과 동일한 구현. */
private class GlowMaxHeap<T>(private val priority: (T) -> Double) {
    private val items = ArrayList<T>()

    fun isEmpty(): Boolean = items.isEmpty()

    fun offer(item: T) {
        items.add(item)
        var i = items.size - 1
        while (i > 0) {
            val parent = (i - 1) / 2
            if (priority(items[parent]) >= priority(items[i])) break
            swap(parent, i)
            i = parent
        }
    }

    fun poll(): T {
        val top = items[0]
        val last = items.removeAt(items.size - 1)
        if (items.isNotEmpty()) {
            items[0] = last
            var i = 0
            while (true) {
                val left = 2 * i + 1
                val right = 2 * i + 2
                var largest = i
                if (left < items.size && priority(items[left]) > priority(items[largest])) largest = left
                if (right < items.size && priority(items[right]) > priority(items[largest])) largest = right
                if (largest == i) break
                swap(i, largest)
                i = largest
            }
        }
        return top
    }

    private fun swap(i: Int, j: Int) {
        val tmp = items[i]
        items[i] = items[j]
        items[j] = tmp
    }
}
