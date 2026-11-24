package com.sloth.runveal.map.snapshot

import com.sloth.runveal.dong.DongPolygonPart
import com.sloth.runveal.map.LatLon

/**
 * 행정동 썸네일(홈 요약 카드)에 쓰는 기하 계산 — 동 폴리곤에서 **범위**, **역마스크**, **외곽선**을
 * 만든다. 썸네일은 동 모양대로 잘리고 그 경계선이 얹히므로 셋을 다 쓴다.
 *
 * 계산은 양 플랫폼이 같아야 해서 commonMain에 둔다.
 */
object DongThumbnailGeometry {

    /** 동이 전부 들어가는 범위. 폴리곤이 비면 null — 호출부가 썸네일을 그리지 않는다. */
    fun bounds(parts: List<DongPolygonPart>): GeoBounds? {
        var south = Double.MAX_VALUE
        var west = Double.MAX_VALUE
        var north = -Double.MAX_VALUE
        var east = -Double.MAX_VALUE
        var any = false
        for (part in parts) {
            for (point in part.exterior) {
                any = true
                if (point.lat < south) south = point.lat
                if (point.lat > north) north = point.lat
                if (point.lon < west) west = point.lon
                if (point.lon > east) east = point.lon
            }
        }
        return if (any) GeoBounds(south = south, west = west, north = north, east = east) else null
    }

    /**
     * 동 **밖**을 덮는 폴리곤 GeoJSON.
     *
     * 동 모양으로 이미지를 자르는 대신, 지도 위에 "동 밖을 가리는 뚜껑"을 덮는다 — 비트맵을 Path로
     * 깎으면 화면 좌표 변환을 직접 해야 하는데, 지도 레이어로 덮으면 좌표계가 지도와 저절로 맞는다.
     *
     * 바깥 링은 [bounds]를 넉넉히 넘기는 사각형이고, 구멍(hole)이 동의 외곽선이다. 동이 여러 조각인
     * 경우(MultiPolygon) 조각마다 구멍을 하나씩 뚫는다.
     */
    fun maskGeoJson(parts: List<DongPolygonPart>, bounds: GeoBounds): String {
        // 회전·여백이 있어도 모자라지 않게 범위를 한참 넓힌 사각형으로 덮는다.
        val pad = maxOf(bounds.north - bounds.south, bounds.east - bounds.west).coerceAtLeast(0.01) * 2
        val outer = listOf(
            LatLon(bounds.south - pad, bounds.west - pad),
            LatLon(bounds.south - pad, bounds.east + pad),
            LatLon(bounds.north + pad, bounds.east + pad),
            LatLon(bounds.north + pad, bounds.west - pad),
            LatLon(bounds.south - pad, bounds.west - pad),
        )
        val holes = parts.map { it.exterior }.filter { it.size >= 3 }
        val rings = (listOf(outer) + holes).joinToString(",") { ring -> ring.toRingJson() }
        return """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[$rings]},"properties":{}}"""
    }

    /** 동 경계선 GeoJSON — 조각마다 선 하나다. */
    fun outlineGeoJson(parts: List<DongPolygonPart>): String {
        val lines = parts.map { it.exterior }.filter { it.size >= 2 }.joinToString(",") { ring ->
            """{"type":"Feature","geometry":{"type":"LineString","coordinates":${ring.toCoordinatesJson()}},"properties":{}}"""
        }
        return """{"type":"FeatureCollection","features":[$lines]}"""
    }

    /** GeoJSON은 [경도, 위도] 순서다 — LatLon과 반대라 여기서 뒤집는다. */
    private fun List<LatLon>.toCoordinatesJson(): String =
        joinToString(",", prefix = "[", postfix = "]") { "[${it.lon},${it.lat}]" }

    private fun List<LatLon>.toRingJson(): String {
        // GeoJSON 링은 첫 점과 끝 점이 같아야 한다.
        val closed = if (first() == last()) this else this + first()
        return closed.toCoordinatesJson()
    }
}

/**
 * 범위의 가운데 좌표. 카메라를 "그 동이 가운데 오게" 옮길 때 쓴다(앱 시작 포커스, 요약 카드의
 * `>` 줌인) — 같은 식이 여러 곳에 흩어지지 않게 한 곳에 둔다.
 */
val GeoBounds.center: LatLon
    get() = LatLon(lat = (south + north) / 2, lon = (west + east) / 2)
