package com.swyp.runveal.map

/**
 * 지도 스타일/카메라/레이어 ID를 한곳에 모은 상수. Android `core:map`과 (추후) iOS `RunvealMap`
 * 패키지가 이 값을 같이 참조해야 두 플랫폼의 레이어 ID·스타일이 어긋나지 않는다.
 */
object MapConfig {
    const val STYLE_URL: String = "https://tiles.openfreemap.org/styles/dark"

    val SEOUL_CITY_HALL: LatLon = LatLon(lat = 37.5665, lon = 126.9780)
    const val INITIAL_ZOOM: Double = 14.0
    /**
     * A3-4(줌 인) 전환 기준. 전환 히스테리시스는 추후 정교화.
     * 동 레이어가 사라지는 기준(홈 탭이 "줌인" 러닝 포커스 UI로 바뀌는 시점)도 이 값을 그대로 쓴다.
     */
    const val ZOOM_IN_THRESHOLD: Double = 14.0

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

    const val SOURCE_SIGUNGU_LINE: String = "sigungu-line-src"
    const val SOURCE_SIGUNGU_LABEL: String = "sigungu-label-src"
    const val LAYER_SIGUNGU_LINE: String = "sigungu-line"
    const val LAYER_SIGUNGU_LABEL: String = "sigungu-label"

    const val SOURCE_SIDO_LINE: String = "sido-line-src"
    const val SOURCE_SIDO_LABEL: String = "sido-label-src"
    const val LAYER_SIDO_LINE: String = "sido-line"
    const val LAYER_SIDO_LABEL: String = "sido-label"

    /** 이 줌보다 확대하면 동(법정동/행정동) 레이어로, 축소하면 시군구 레이어로 바뀐다. 기기 실측으로 정함. */
    const val SIGUNGU_SWITCH_ZOOM: Double = 9.6

    /** 이 줌보다 확대하면 시군구 레이어로, 축소하면 시도(광역시·도) 레이어로 바뀐다. */
    const val SIDO_SWITCH_ZOOM: Double = 8.0

    /**
     * 기본 스타일([STYLE_URL], OpenFreeMap dark)에 내장된 지명(시/군/구/읍/면/동 등) 라벨 레이어 ID.
     * 동 레이어가 나타나는 시점([SIGUNGU_SWITCH_ZOOM])부터는 우리 동/시군구 라벨과 겹쳐 보이지
     * 않게 이 레이어들을 숨긴다.
     */
    val BASE_STYLE_PLACE_LABEL_LAYERS: List<String> = listOf(
        "place_other",
        "place_suburb",
        "place_village",
        "place_town",
        "place_city",
        "place_city_large",
        "place_state",
    )

    /** 기본 스타일에 내장된 도로 이름 라벨 레이어 ID. [BASE_STYLE_PLACE_LABEL_LAYERS]와 같은 기준으로 숨긴다. */
    val BASE_STYLE_ROAD_LABEL_LAYERS: List<String> = listOf(
        "highway_name_other",
        "highway_name_motorway",
    )
}
