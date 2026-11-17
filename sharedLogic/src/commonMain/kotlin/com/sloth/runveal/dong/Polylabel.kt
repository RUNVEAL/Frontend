package com.sloth.runveal.dong

import com.sloth.runveal.map.LatLon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 폴리곤(구멍 포함)에서 "접근 불가능한 극"(pole of inaccessibility, 경계에서 가장 먼 내부 점)을 찾는다.
 * 오목한 동 모양에서 단순 중심점(꼭짓점 평균)을 쓰면 라벨이 영역 밖으로 나갈 수 있어서, 라벨 위치는
 * 항상 이걸로 잡는다. Mapbox의 polylabel 알고리즘(격자 셀을 우선순위 큐로 계속 4등분하며 가장 유망한
 * 셀부터 본다)을 순수 Kotlin으로 옮긴 것 — 플랫폼 의존이 없어 commonTest가 가능하다.
 */
object Polylabel {
    private const val EARTH_RADIUS_M = 6371000.0
    private const val SQRT2 = 1.4142135623730951
    private const val MAX_ITERATIONS = 20000

    /**
     * [exterior](+[holes])로 이뤄진 폴리곤 하나의 극을 찾는다. 위경도 그대로 받되, 내부 계산은
     * 외곽 ring 꼭짓점의 평균 위도를 기준으로 평면(m) 좌표에 투영해서 한다 — 행정동 하나 크기(수 km)
     * 에서는 이 평면 근사로 충분하다. [precisionMeters]는 더 세밀하게 쪼개는 걸 멈추는 기준(작을수록
     * 정확하지만 느려진다).
     */
    fun pole(exterior: List<LatLon>, holes: List<List<LatLon>> = emptyList(), precisionMeters: Double = 10.0): LatLon {
        require(exterior.size >= 3) { "exterior ring needs at least 3 points" }
        val lat0 = exterior.sumOf { it.lat } / exterior.size
        val lon0 = exterior.sumOf { it.lon } / exterior.size
        val lonScale = cos(toRadians(lat0)).let { if (it == 0.0) 1e-9 else it }

        fun toX(lon: Double) = toRadians(lon - lon0) * lonScale * EARTH_RADIUS_M
        fun toY(lat: Double) = toRadians(lat - lat0) * EARTH_RADIUS_M
        fun fromXY(x: Double, y: Double) = LatLon(
            lat = lat0 + toDegrees(y / EARTH_RADIUS_M),
            lon = lon0 + toDegrees(x / (EARTH_RADIUS_M * lonScale)),
        )

        val rings = (listOf(exterior) + holes).map { ring -> ring.map { doubleArrayOf(toX(it.lon), toY(it.lat)) } }
        val exteriorXY = rings[0]

        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (p in exteriorXY) {
            if (p[0] < minX) minX = p[0]
            if (p[0] > maxX) maxX = p[0]
            if (p[1] < minY) minY = p[1]
            if (p[1] > maxY) maxY = p[1]
        }
        val width = maxX - minX
        val height = maxY - minY
        if (width <= 0.0 || height <= 0.0) return fromXY((minX + maxX) / 2, (minY + maxY) / 2)

        fun distToRings(x: Double, y: Double): Double {
            var inside = false
            var minDistSq = Double.MAX_VALUE
            for (ring in rings) {
                var j = ring.size - 1
                for (i in ring.indices) {
                    val a = ring[i]
                    val b = ring[j]
                    if ((a[1] > y) != (b[1] > y) && x < (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1]) + a[0]) {
                        inside = !inside
                    }
                    minDistSq = minOf(minDistSq, segDistSq(x, y, a, b))
                    j = i
                }
            }
            val d = sqrt(minDistSq)
            return if (inside) d else -d
        }

        // 셀 하나 = 후보 지점(x,y) + "이 셀 안 어딘가에서 나올 수 있는 거리의 상한"(max). max가 큰 셀부터
        // 본다(우선순위 큐) — 가망 없는 셀은 더 쪼개지 않고 버려서 전체 그리드를 다 훑지 않아도 된다.
        class Cell(val x: Double, val y: Double, val h: Double) {
            val d = distToRings(x, y)
            val max = d + h * SQRT2
        }

        val cellSize = minOf(width, height)
        var h = cellSize / 2
        val queue = MaxHeap<Cell> { it.max }
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
        val centroidX = exteriorXY.sumOf { it[0] } / exteriorXY.size
        val centroidY = exteriorXY.sumOf { it[1] } / exteriorXY.size
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
        return fromXY(best.x, best.y)
    }

    private fun segDistSq(px: Double, py: Double, a: DoubleArray, b: DoubleArray): Double {
        var x = a[0]
        var y = a[1]
        var dx = b[0] - x
        var dy = b[1] - y
        if (dx != 0.0 || dy != 0.0) {
            val t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy)
            if (t > 1) {
                x = b[0]; y = b[1]
            } else if (t > 0) {
                x += dx * t; y += dy * t
            }
        }
        dx = px - x
        dy = py - y
        return dx * dx + dy * dy
    }

    private fun toRadians(degrees: Double): Double = degrees / 180.0 * PI
    private fun toDegrees(radians: Double): Double = radians / PI * 180.0
}

/** `java.util.PriorityQueue`가 없는 commonMain용 최소 배열 기반 최대 힙. [priority]가 큰 원소부터 [poll]된다. */
private class MaxHeap<T>(private val priority: (T) -> Double) {
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
