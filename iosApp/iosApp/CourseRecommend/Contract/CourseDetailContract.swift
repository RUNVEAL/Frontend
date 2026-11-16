import Foundation
import SharedLogic

enum CourseDetailIntent: UiIntent {
    /// 화면이 뜰 때 한 번 — 홀더에서 이 id로 코스를 다시 찾아온다(화면 인자로 전체 응답을 못 넘겨서).
    case load(routeId: String)
    /// "코스 저장" 버튼 — 지금 보고 있는 코스를 로컬 `course` 테이블에 저장한다.
    case saveCourse
}

/// [RunvealRouteStep] 한 줄.
struct RouteStepUiModel: Identifiable {
    let id = UUID()
    let place: String
    let note: String?
    let distanceLabel: String
    let type: RunvealRouteStepType
}

enum CourseDetailScreenState: UiState {
    case loading
    /// 받은 routeId를 홀더에서 못 찾았을 때 — 추천 결과가 이미 사라진 경우.
    case notFound
    case ready(CourseDetailReady)
}

struct CourseDetailReady {
    /// "A", "B" … 라벨의 글자 부분. 전체 문구는 화면에서 조합한다.
    let courseLabel: String
    let distanceValue: String
    let distanceUnit: String
    let timeValue: String
    let timeUnit: String
    let elevationValue: String
    let elevationUnit: String
    let newRoadValue: String
    let newRoadUnit: String
    let elevationProfile: [ElevationPoint]
    let routeSteps: [RouteStepUiModel]
    /// `RouteDto.path`를 지도에 그릴 수 있게 변환한 좌표.
    let path: [LatLon]
    let isLoop: Bool
    /// 로컬 `course` 테이블에 저장했는지 — 중복 저장 방지 + 버튼 문구에 쓴다.
    var isSaved = false
}
