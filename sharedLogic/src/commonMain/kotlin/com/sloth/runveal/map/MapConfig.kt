package com.sloth.runveal.map

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

    /** 러닝을 시작할 때 맞추는 줌 — 지금 달리는 길과 다음 갈림길이 같이 보이는 정도. */
    const val RECORDING_ZOOM: Double = 16.5

    /**
     * 다른 화면에서 홈 줌인 뷰로 보낼 때 맞추는 줌(기록 탭 "러닝 시작하기" 등) — 주변 도로와 동네
     * 범위가 같이 보이는 정도다.
     *
     * [ZOOM_IN_THRESHOLD]보다 넉넉히 위에 둔다. 임계값에 딱 맞추면 전환 히스테리시스가 없어서
     * 손가락이 살짝 스쳐 축소되는 것만으로 줌아웃 UI로 되돌아간다.
     */
    const val HOME_ZOOM_IN: Double = 15.0

    val KOREA_BOUNDS_NORTH_WEST: LatLon = LatLon(lat = 38.7, lon = 124.5)
    val KOREA_BOUNDS_SOUTH_EAST: LatLon = LatLon(lat = 33.0, lon = 131.0)
    const val MIN_ZOOM_PREFERENCE: Double = 6.0

    const val SOURCE_VISITED_ROAD: String = "visited"
    const val LAYER_VISITED_ROAD_GLOW_OUTER: String = "visited-glow-outer"
    const val LAYER_VISITED_ROAD_GLOW_MID: String = "visited-glow-mid"
    const val LAYER_VISITED_ROAD_LINE: String = "visited-line"

    /** 러닝 중 실시간 선 — 지금까지 달린 경로 중 이미 가본 구간(흰색, glow 없음). [VisitedRoadLayer]를
     * [SOURCE_VISITED_ROAD]와 다른 소스로 한 번 더 적용한다. */
    const val SOURCE_LIVE_RUN_OLD: String = "live-run-old"

    /** 러닝 중 실시간 선 — 지금까지 달린 경로 중 처음 가보는 구간(금색 + glow, 홈 지도 "새로 탐험한
     * 길"과 같은 톤). */
    const val SOURCE_LIVE_RUN_NEW: String = "live-run-new"

    /** 코스 추천 결과 — 선택 안 된 코스/선택된 코스를 `selected` 피처 속성으로 나눠 그린다(레이어 ID는
     * `RecommendedRouteLayer`가 이 소스 ID에서 그대로 파생시킨다). */
    const val SOURCE_RECOMMENDED_ROUTE: String = "recommended-route"

    /** 코스 상세 화면이 그리는 단일 코스 선 — [SOURCE_RECOMMENDED_ROUTE]와 소스를 공유하면, 결과
     * 화면(여러 코스) ↔ 상세 화면(코스 하나) 전환 시점에 둘의 Compose 생명주기가 겹치면서 한쪽이
     * 지우는 게 다른 쪽이 막 그린 걸 지워버리는 경합이 생겨 선이 사라져 보일 수 있다. 소스를
     * 분리해서 그 경합 자체를 없앤다. */
    const val SOURCE_COURSE_DETAIL_ROUTE: String = "course-detail-route"

    /** 러닝 중 지도에 그리는 "이 코스로 러닝 시작"의 추천 코스 안내선 — 흰색 점선 하나뿐이라
     * [SOURCE_RECOMMENDED_ROUTE]/[SOURCE_COURSE_DETAIL_ROUTE]와 별도 소스로 둔다. */
    const val SOURCE_RECORDING_COURSE_GUIDE: String = "recording-course-guide"

    const val SOURCE_DONG_FILL: String = "dong-fill-src"
    const val SOURCE_DONG_LINE: String = "dong-line-src"
    const val SOURCE_DONG_LABEL: String = "dong-label-src"
    const val LAYER_DONG_FILL: String = "dong-fill"
    const val LAYER_DONG_LINE: String = "dong-line"
    const val LAYER_DONG_LABEL: String = "dong-label"

    /** 지도 가운데 보이는 "메인" 행정동 라벨 — 같은 소스를 쓰지만 더 크고 굵게 그린다.
     * 한 레이어에서 피처별로 글꼴을 바꿀 수 없어 레이어를 따로 둔다. */
    const val LAYER_DONG_LABEL_MAIN: String = "dong-label-main"

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
        "place_country_other",
        "place_country_minor",
        "place_country_major",
    )

    /** 기본 스타일에 내장된 도로 이름 라벨 레이어 ID. [BASE_STYLE_PLACE_LABEL_LAYERS]와 같은 기준으로 숨긴다. */
    val BASE_STYLE_ROAD_LABEL_LAYERS: List<String> = listOf(
        "highway_name_other",
        "highway_name_motorway",
    )

    /** 기본 스타일에 내장된 행정구역 경계선(시/도·국가 테두리) 레이어 ID. 라벨과 별개로 항상 켜져
     * 있어서, 우리 동/시군구/시도 테두리([DongOverlayLayer] 등)와 겹쳐 보이지 않게 같이 숨긴다. */
    val BASE_STYLE_BOUNDARY_LAYERS: List<String> = listOf(
        "boundary_state",
        "boundary_country_z0-4",
        "boundary_country_z5-",
    )

    /**
     * 베이스 타일의 POI 레이어(OpenMapTiles `poi`). **스타일에는 정의가 없고 타일에만 들어 있어서**,
     * 쓰려면 레이어를 직접 추가해야 한다([LAYER_BASE_LANDCOVER_GRASS]와 같은 방식).
     */
    const val SOURCE_LAYER_BASE_POI: String = "poi"

    /** 줌인에서만 띄우는 주요 장소 이름 레이어 — 우리가 추가하는 것이라 ID도 우리 것이다. */
    const val LAYER_BASE_POI_LABEL: String = "runveal-poi-label"

    /**
     * 이름을 띄울 POI 종류(`poi.subclass`) — 역과 터미널만.
     *
     * 버스 정류장(`bus_stop`)이나 상점까지 켜면 줌인 화면이 글자로 덮여 도로와 가본 길이 안 보인다.
     * 지금 필요한 건 "여기가 어디쯤인지" 알려주는 큰 기준점뿐이다.
     */
    val BASE_POI_LABEL_SUBCLASSES: List<String> = listOf("station", "bus_station")

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

    /**
     * 기본 스타일의 도로 선 레이어 ID 전체 — 보행로/이면도로부터 간선/고속도로까지. 철도(`railway_*`)와
     * 부두(`road_pier`)는 도로가 아니라 제외한다. "안 가본 길" 보기에서 이 레이어들의 색을 한 번에
     * 바꿔 도로를 강조한다. `*_casing`은 `*_inner` 아래에 더 두껍게 깔리는 외곽선이라, 둘 다 같은 색으로
     * 덮어야 도로가 테두리 없는 한 덩어리 선으로 보인다.
     */
    val BASE_STYLE_ROAD_LAYERS: List<String> = listOf(
        "highway_path",
        "highway_minor",
    ) + BASE_STYLE_MAJOR_ROAD_LAYERS
}
