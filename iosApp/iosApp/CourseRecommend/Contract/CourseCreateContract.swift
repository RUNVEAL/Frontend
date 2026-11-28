import Foundation
import SharedLogic

enum CourseCreateIntent: UiIntent {
    case startLocationClicked
    case endLocationClicked
    case cancelLocationSelection
    case confirmLocationSelection
    /// 출발/도착 선택 모드에서 지도를 탭했을 때 — 확정 전까지의 임시 좌표.
    case mapLocationPicked(LatLon)

    /// 화면이 받아 둔 현재 위치 — 고른 자리까지의 거리를 재는 데 쓴다(못 받았으면 nil).
    case currentLocationResolved(LatLon?)
    case openFilterSheet
    case closeFilterSheet
    case selectPresetDistance(String)
    case selectCustomDistanceChip
    case changeCustomDistanceIndex(Int)
    case setAvoidVisitedPreferred(Bool)
    case toggleFilter(String)
    case resetFilterSheet
    case applyFilterSheet
    /// 출발지를 한 번도 안 골랐을 때 쓸 현재 위치 — 화면이 미리 받아서 같이 넘긴다(권한/위치 서비스는
    /// ViewModel이 몰라도 되게). 못 받았으면 nil.
    case recommendClicked(currentLocation: LatLon?)
}

enum CourseCreateEffect: UiEffect {
    /// `/api/v1/routes` 응답을 받은 뒤(성공/실패 모두) 결과 화면으로 넘어간다.
    case navigateToResult
}

/// 상단 앱바 제목을 바꾸는 출발/도착 선택 모드. 선택 중이 아니면 `none`.
enum LocationSelectionTarget {
    case none
    case start
    case end
}

enum CourseCreateOptions {
    static let distanceOptions = ["3km", "5km", "7km", "10km"]
    static let defaultDistance = "5km"
    static let filterOptions = ["신호/횡단보도 적은 길", "완만한 길", "공원 지나가기", "하천길 따라가기"]

    /// 1km ~ 21km, 0.5km 단위. 인덱스 22 = "12" (휠 피커 기본값).
    static let customDistanceValues: [String] = (2...42).map { step in
        let km = Double(step) / 2
        return km == km.rounded() ? "\(Int(km))" : "\(km)"
    }
    static let defaultCustomDistanceIndex = 22

    /// 거리를 아직 잴 수 없을 때 — 단위를 남겨 둬서 그 자리가 거리라는 건 보이게 한다(Android와 같다).
    static let distancePlaceholder = "-km"

    /// 동 이름을 아직 못 받았을 때(Android와 같다).
    static let placeNamePlaceholder = "-"
}

struct CourseCreateState: UiState {
    var startLocation = CourseRecommendStrings.currentLocation
    var endLocation = CourseRecommendStrings.returnToStart
    var endLocationIsDefault = true
    var locationSelectionTarget: LocationSelectionTarget = .none
    /// 지도를 탭해 고른 실제 좌표(확정 전) — 확정하면 [startLatLon]/[endLatLon]으로 복사된다.
    var pickedLocation: LatLon?
    var startLatLon: LatLon?
    var endLatLon: LatLon?
    /// [pickedLocation]이 속한 행정동 이름(`GET /api/v1/dong`) — 아직 못 받았거나 조회가 실패하면
    /// nil이고, 그때는 [selectedPlaceName]이 플레이스홀더를 보여준다.
    var pickedPlaceName: String?
    /// 화면이 받아 둔 현재 위치 — [selectedPlaceDistance]를 재는 기준점이다. 권한이 없거나 위치가
    /// 꺼져 있으면 nil이고, 그때는 거리를 안 보여준다.
    var currentLocation: LatLon?
    var selectedDistance = CourseCreateOptions.defaultDistance
    var isCustomDistanceSelected = false
    var customDistanceIndex = CourseCreateOptions.defaultCustomDistanceIndex
    var avoidVisitedPreferred = true
    var selectedFilters: Set<String> = []
    var showFilterSheet = false

    var distanceLabel: String {
        isCustomDistanceSelected
            ? "\(CourseCreateOptions.customDistanceValues[customDistanceIndex])km"
            : selectedDistance
    }

    /// 탭한 자리의 이름([pickedPlaceName]) — 도로명이 있으면 "한보라2로", 없으면 "중구 명동".
    ///
    /// 조회 중이거나 실패했으면 [CourseCreateOptions.placeNamePlaceholder]를 둔다 — 예전에는 좌표를
    /// 그대로 보여줬지만 "37.5665, 126.9780"은 읽는 사람에게 아무 뜻이 없어서 이름이 들어올
    /// 자리라는 것만 알린다.
    ///
    /// 아직 아무 자리도 안 골랐을 때는 플레이스홀더가 아니라 **할 일을 알려주는 안내**를 쓴다.
    ///
    /// 확정하면 이 값이 그대로 출발지/도착지 라벨로 들어간다.
    var selectedPlaceName: String {
        if let pickedPlaceName { return pickedPlaceName }
        guard pickedLocation != nil else { return CourseRecommendStrings.pickLocation }
        return CourseCreateOptions.placeNamePlaceholder
    }

    /**
     현재 위치에서 고른 자리까지의 거리 — 선택 패널에서 동 이름 아래 줄에 쓴다.

     아직 잴 수 없을 때도(현재 위치를 못 받았거나 자리를 안 골랐을 때) 줄을 비우지 않고
     [CourseCreateOptions.distancePlaceholder]를 둔다 — 줄이 사라지면 사용자가 그 자리가 거리인지
     알 수 없다.

     **직선거리**다(`GeoMath`) — 실제로 달릴 경로 길이가 아니다. 길을 따라가는 거리는 서버에
     물어야 하는데, 여기서 필요한 건 "고른 자리가 지금 얼마나 먼지" 감을 주는 것뿐이다.
     Android와 같은 계산·같은 문구를 쓴다.
     */
    var selectedPlaceDistance: String {
        guard let from = currentLocation, let to = pickedLocation else {
            return CourseCreateOptions.distancePlaceholder
        }
        let meters = IosGeo.shared.distanceMeters(from: from, to: to)
        return String(format: "%.1fkm", meters / 1000)
    }

    var topBarTitle: String {
        switch locationSelectionTarget {
        case .none: return CourseRecommendStrings.createTitle
        case .start: return CourseRecommendStrings.selectStartTitle
        case .end: return CourseRecommendStrings.selectEndTitle
        }
    }

    var locationConfirmLabel: String {
        switch locationSelectionTarget {
        case .none: return ""
        case .start: return CourseRecommendStrings.confirmStart
        case .end: return CourseRecommendStrings.confirmEnd
        }
    }

    var locationHintMessage: String {
        switch locationSelectionTarget {
        case .none: return ""
        case .start: return CourseRecommendStrings.hintPickStart
        case .end: return CourseRecommendStrings.hintPickEnd
        }
    }
}
