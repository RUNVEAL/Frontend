package com.swyp.runveal.map

/**
 * 지도 스타일/카메라/레이어 ID를 한곳에 모은 상수. Android `core:map`과 (추후) iOS `RunvealMap`
 * 패키지가 이 값을 같이 참조해야 두 플랫폼의 레이어 ID·스타일이 어긋나지 않는다.
 */
object MapConfig {
    const val STYLE_URL: String = "https://tiles.openfreemap.org/styles/dark"

    val SEOUL_CITY_HALL: LatLon = LatLon(lat = 37.5665, lon = 126.9780)
    const val INITIAL_ZOOM: Double = 13.0
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

    /** 코스 추천 결과 — 선택 안 된 코스/선택된 코스를 `selected` 피처 속성으로 나눠 그린다(레이어 ID는
     * `RecommendedRouteLayer`가 이 소스 ID에서 그대로 파생시킨다). */
    const val SOURCE_RECOMMENDED_ROUTE: String = "recommended-route"

    /** 코스 상세 화면이 그리는 단일 코스 선 — [SOURCE_RECOMMENDED_ROUTE]와 소스를 공유하면, 결과
     * 화면(여러 코스) ↔ 상세 화면(코스 하나) 전환 시점에 둘의 Compose 생명주기가 겹치면서 한쪽이
     * 지우는 게 다른 쪽이 막 그린 걸 지워버리는 경합이 생겨 선이 사라져 보일 수 있다. 소스를
     * 분리해서 그 경합 자체를 없앤다. */
    const val SOURCE_COURSE_DETAIL_ROUTE: String = "course-detail-route"

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

    /** 이 줌보다 확대하면 동(행정동) 레이어로, 축소하면 시군구 레이어로 바뀐다. 기기 실측으로 정함. */
    const val SIGUNGU_SWITCH_ZOOM: Double = 9.6

    /** 이 줌보다 확대하면 시군구 레이어로, 축소하면 시도(광역시·도) 레이어로 바뀐다. */
    const val SIDO_SWITCH_ZOOM: Double = 8.0

    /**
     * 기본 스타일([STYLE_URL], OpenFreeMap dark)에 내장된 지명(시/군/구/읍/면/동 등) 라벨 레이어 ID.
     * 모든 줌에서 우리 동/시군구/시도 라벨과 겹쳐 보이지 않게 이 레이어들을 항상 숨긴다.
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

    /** 기본 스타일의 vector 소스 ID. [LAYER_BASE_LANDCOVER_GRASS]처럼 새 레이어를 추가할 때 필요하다. */
    const val SOURCE_BASE_STYLE: String = "openmaptiles"

    const val LAYER_BASE_BUILDING: String = "building"
    const val LAYER_BASE_LANDUSE_PARK: String = "landuse_park"
    const val LAYER_BASE_LANDCOVER_WOOD: String = "landcover_wood"

    /** 다크 스타일엔 원래 없는 레이어(liberty 스타일에만 있음) — 러닝 포커스에서 새로 추가해 그린다. */
    const val LAYER_BASE_LANDCOVER_GRASS: String = "landcover_grass"

    /**
     * 기본 스타일의 간선/고속도로 레이어 ID. 동/시군구/시도 뷰([ZOOM_IN_THRESHOLD] 미만)에서는 일반
     * 도로보다 더 흐리게 깔아서, 지금 보여주는 게 길이 아니라 동네 경계라는 걸 분명히 한다.
     */
    val BASE_STYLE_MAJOR_ROAD_LAYERS: List<String> = listOf(
        "highway_major_casing",
        "highway_major_inner",
        "highway_major_subtle",
        "highway_motorway_casing",
        "highway_motorway_inner",
        "highway_motorway_subtle",
    )
}
