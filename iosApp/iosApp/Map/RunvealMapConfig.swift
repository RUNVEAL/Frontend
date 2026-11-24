import Foundation
import SharedLogic

/// sharedLogic `MapConfig`(두 플랫폼 공용 지도 상수)를 Swift에서 쓰기 좋게 한 번 감싼 것.
///
/// 값 자체는 전부 Kotlin 쪽 한 곳에서 온다 — 여기에 숫자를 다시 적지 않는다. Kotlin/Native가
/// `const val`을 내보내는 이름(대문자 스네이크)에 기대는 코드가 여기에만 모이게 하는 목적도 있다.
enum RunvealMapConfig {
    private static var config: MapConfig { MapConfig.shared }

    static let styleURL = URL(string: MapConfig.shared.STYLE_URL)

    static var seoulCityHall: LatLon { config.SEOUL_CITY_HALL }
    static var initialZoom: Double { config.INITIAL_ZOOM }
    /// 러닝을 시작할 때 맞추는 줌 — 달리는 길이 보일 만큼 가깝게.
    static var recordingZoom: Double { config.RECORDING_ZOOM }
    /// A3-4(줌 인) 전환 기준. 동 레이어가 사라지는 기준도 같은 값이다.
    static var zoomInThreshold: Double { config.ZOOM_IN_THRESHOLD }
    /// 다른 탭에서 홈 줌인 뷰로 보낼 때 맞추는 줌 — [zoomInThreshold]보다 넉넉히 위다.
    static var homeZoomIn: Double { config.HOME_ZOOM_IN }
    static var sigunguSwitchZoom: Double { config.SIGUNGU_SWITCH_ZOOM }
    static var sidoSwitchZoom: Double { config.SIDO_SWITCH_ZOOM }
    static var minZoomPreference: Double { config.MIN_ZOOM_PREFERENCE }
    static var koreaBoundsNorthWest: LatLon { config.KOREA_BOUNDS_NORTH_WEST }
    static var koreaBoundsSouthEast: LatLon { config.KOREA_BOUNDS_SOUTH_EAST }

    static var sourceVisitedRoad: String { config.SOURCE_VISITED_ROAD }
    static var sourceLiveRunOld: String { config.SOURCE_LIVE_RUN_OLD }
    static var sourceLiveRunNew: String { config.SOURCE_LIVE_RUN_NEW }
    static var sourceRecommendedRoute: String { config.SOURCE_RECOMMENDED_ROUTE }
    static var sourceCourseDetailRoute: String { config.SOURCE_COURSE_DETAIL_ROUTE }
    static var sourceRecordingCourseGuide: String { config.SOURCE_RECORDING_COURSE_GUIDE }

    static var layerVisitedRoadLine: String { config.LAYER_VISITED_ROAD_LINE }

    static var sourceDongFill: String { config.SOURCE_DONG_FILL }
    static var sourceDongLine: String { config.SOURCE_DONG_LINE }
    static var sourceDongLabel: String { config.SOURCE_DONG_LABEL }
    static var layerDongFill: String { config.LAYER_DONG_FILL }
    static var layerDongLine: String { config.LAYER_DONG_LINE }
    static var layerDongLabel: String { config.LAYER_DONG_LABEL }

    static var sourceSigunguLine: String { config.SOURCE_SIGUNGU_LINE }
    static var sourceSigunguLabel: String { config.SOURCE_SIGUNGU_LABEL }
    static var layerSigunguLine: String { config.LAYER_SIGUNGU_LINE }
    static var layerSigunguLabel: String { config.LAYER_SIGUNGU_LABEL }

    static var sourceSidoLine: String { config.SOURCE_SIDO_LINE }
    static var sourceSidoLabel: String { config.SOURCE_SIDO_LABEL }
    static var layerSidoLine: String { config.LAYER_SIDO_LINE }
    static var layerSidoLabel: String { config.LAYER_SIDO_LABEL }

    static var baseStylePlaceLabelLayers: [String] { config.BASE_STYLE_PLACE_LABEL_LAYERS }
    static var baseStyleRoadLabelLayers: [String] { config.BASE_STYLE_ROAD_LABEL_LAYERS }
    static var baseStyleBoundaryLayers: [String] { config.BASE_STYLE_BOUNDARY_LAYERS }
    static var baseStyleMajorRoadLayers: [String] { config.BASE_STYLE_MAJOR_ROAD_LAYERS }
    static var baseStyleRoadLayers: [String] { config.BASE_STYLE_ROAD_LAYERS }

    static var sourceBaseStyle: String { config.SOURCE_BASE_STYLE }
    static var layerBaseBuilding: String { config.LAYER_BASE_BUILDING }
    /// 베이스 타일의 POI 레이어 — 스타일에는 정의가 없고 타일에만 있어서 직접 레이어를 만든다.
    static var sourceLayerBasePoi: String { config.SOURCE_LAYER_BASE_POI }
    static var layerBasePoiLabel: String { config.LAYER_BASE_POI_LABEL }
    static var basePoiLabelSubclasses: [String] { config.BASE_POI_LABEL_SUBCLASSES }
    static var layerBaseLandusePark: String { config.LAYER_BASE_LANDUSE_PARK }
    static var layerBaseLandcoverWood: String { config.LAYER_BASE_LANDCOVER_WOOD }
    static var layerBaseLandcoverGrass: String { config.LAYER_BASE_LANDCOVER_GRASS }
}
