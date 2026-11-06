package com.swyp.runveal.map

/**
 * 지도 스타일/카메라/레이어 ID를 한곳에 모은 상수. Android `core:map`과 (추후) iOS `RunvealMap`
 * 패키지가 이 값을 같이 참조해야 두 플랫폼의 레이어 ID·스타일이 어긋나지 않는다.
 */
object MapConfig {
    const val STYLE_URL: String = "https://tiles.openfreemap.org/styles/dark"

    val SEOUL_CITY_HALL: LatLon = LatLon(lat = 37.5665, lon = 126.9780)
    const val INITIAL_ZOOM: Double = 14.0
    /** A3-4(줌 인) 전환 기준. 전환 히스테리시스(16.0 / 15.7)는 추후 정교화. */
    const val ZOOM_IN_THRESHOLD: Double = 16.0

    val KOREA_BOUNDS_NORTH_WEST: LatLon = LatLon(lat = 38.7, lon = 124.5)
    val KOREA_BOUNDS_SOUTH_EAST: LatLon = LatLon(lat = 33.0, lon = 131.0)
    const val MIN_ZOOM_PREFERENCE: Double = 6.0

    const val SOURCE_VISITED_ROAD: String = "visited"
    const val LAYER_VISITED_ROAD_GLOW_OUTER: String = "visited-glow-outer"
    const val LAYER_VISITED_ROAD_GLOW_MID: String = "visited-glow-mid"
    const val LAYER_VISITED_ROAD_LINE: String = "visited-line"

    const val SOURCE_DONG_FILL: String = "dong-fill-src"
    const val SOURCE_DONG_LINE: String = "dong-line-src"
    const val SOURCE_DONG_LABEL: String = "dong-label-src"
    const val LAYER_DONG_FILL: String = "dong-fill"
    const val LAYER_DONG_LINE: String = "dong-line"
    const val LAYER_DONG_LABEL: String = "dong-label"
}
