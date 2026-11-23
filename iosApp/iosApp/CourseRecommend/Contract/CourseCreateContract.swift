import Foundation
import SharedLogic

enum CourseCreateIntent: UiIntent {
    case startLocationClicked
    case endLocationClicked
    case cancelLocationSelection
    case confirmLocationSelection
    /// 출발/도착 선택 모드에서 지도를 탭했을 때 — 확정 전까지의 임시 좌표.
    case mapLocationPicked(LatLon)
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

    /// 1km ~ 42km, 0.5km 단위. 인덱스 22 = "12" (휠 피커 기본값).
    static let customDistanceValues: [String] = (2...84).map { step in
        let km = Double(step) / 2
        return km == km.rounded() ? "\(Int(km))" : "\(km)"
    }
    static let defaultCustomDistanceIndex = 22
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
    /// nil이고, 그때는 [selectedPlaceName]이 좌표를 그대로 보여준다.
    var pickedPlaceName: String?
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

    /// 탭한 자리의 행정동 이름([pickedPlaceName], 예: "중구 명동"). 조회 중이거나 실패했으면 좌표를
    /// 그대로 보여준다 — 이름이 안 떠도 어디를 찍었는지는 알 수 있어야 한다.
    ///
    /// 확정하면 이 값이 그대로 출발지/도착지 라벨로 들어간다.
    var selectedPlaceName: String {
        if let pickedPlaceName { return pickedPlaceName }
        guard let pickedLocation else { return CourseRecommendStrings.pickLocation }
        return String(format: "%.4f, %.4f", pickedLocation.lat, pickedLocation.lon)
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
