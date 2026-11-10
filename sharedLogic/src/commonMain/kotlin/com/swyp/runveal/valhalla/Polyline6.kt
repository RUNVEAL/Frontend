package com.swyp.runveal.valhalla

import com.swyp.runveal.map.LatLon
import kotlin.math.round

/** `feature-valhalla-matching.md` "polyline6" — Valhalla가 쓰는 polyline 인코딩. Google 기본
 * polyline(정밀도 1e5)과 비트 연산 구조는 같지만 정밀도가 1e6이라 섞어 쓰면 좌표가 틀어진다. */
object Polyline6 {
    private const val PRECISION = 1e6

    fun decode(encoded: String): List<LatLon> {
        val coordinates = mutableListOf<LatLon>()
        var index = 0
        var lat = 0L
        var lon = 0L

        while (index < encoded.length) {
            val (deltaLat, nextIndex1) = decodeDelta(encoded, index)
            index = nextIndex1
            val (deltaLon, nextIndex2) = decodeDelta(encoded, index)
            index = nextIndex2

            lat += deltaLat
            lon += deltaLon
            coordinates.add(LatLon(lat = lat / PRECISION, lon = lon / PRECISION))
        }
        return coordinates
    }

    /** [startIndex]부터 가변 길이로 인코딩된 델타값 하나를 읽는다. 반환: (델타, 다음 시작 인덱스). */
    private fun decodeDelta(encoded: String, startIndex: Int): Pair<Long, Int> {
        var index = startIndex
        var shift = 0
        var result = 0L
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f).toLong() shl shift)
            shift += 5
        } while (b >= 0x20)
        val delta = if (result and 1L != 0L) (result shr 1).inv() else (result shr 1)
        return delta to index
    }

    fun encode(coordinates: List<LatLon>): String {
        val builder = StringBuilder()
        var prevLat = 0L
        var prevLon = 0L
        for (point in coordinates) {
            val lat = round(point.lat * PRECISION).toLong()
            val lon = round(point.lon * PRECISION).toLong()
            encodeValue(lat - prevLat, builder)
            encodeValue(lon - prevLon, builder)
            prevLat = lat
            prevLon = lon
        }
        return builder.toString()
    }

    private fun encodeValue(value: Long, builder: StringBuilder) {
        var v = value shl 1
        if (value < 0) v = v.inv()
        while (v >= 0x20) {
            builder.append(((0x20 or (v and 0x1f).toInt()) + 63).toChar())
            v = v shr 5
        }
        builder.append((v.toInt() + 63).toChar())
    }
}
