import Foundation
import SharedLogic

/// 코스 상세가 보여줄 코스를 **어디서 읽을지**. 같은 화면을 두 경로에서 쓰는데 들고 있는 데이터가
/// 서로 달라서 id만으로는 구분할 수 없다(Android `CourseDetailSource`).
enum CourseDetailSource: Hashable {
    /// 추천 결과에서 카드를 눌러 들어온 코스 — 아직 저장 전일 수 있다. 네비게이션 인자로 전체
    /// 응답을 못 넘기므로 `CourseRecommendResultHolder`에서 이 id로 다시 찾는다.
    case recommended(id: String)
    /// "내 코스"에서 눌러 들어온, 이미 저장된 코스 — 로컬 `course` 테이블에서 읽는다.
    case saved(id: String)

    /// 출처와 무관하게 "이 코스"를 가리키는 id — 지도 레이어의 피처 id처럼 어느 쪽이든 같은 값을
    /// 쓰는 자리가 있다(Android `CourseDetailSource.id`).
    var id: String {
        switch self {
        case .recommended(let id), .saved(let id): return id
        }
    }
}

enum CourseDetailIntent: UiIntent {
    /// 화면이 뜰 때 한 번 — [source]가 정한 곳에서 이 코스를 읽어 온다.
    case load(source: CourseDetailSource)
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
    /// 코스를 못 찾았을 때 — 추천 결과가 이미 사라졌거나, 저장된 코스가 지워진 경우.
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
    /**
     로컬 `course` 테이블에 있는 이 코스의 id — 없으면(저장 전 추천 코스) nil.

     "내 코스"에서 들어왔으면 들어올 때부터 있고, 추천 결과에서 **이 화면에서 저장하면** 그때
     채워진다. 러닝이 끝나 기록이 저장될 때 이 코스를 "달림"으로 적는 데 쓴다 — 출처(`source`)로
     가리면 "저장하고 바로 옆 시작 버튼"을 누른 경우를 놓친다.
     */
    var savedCourseId: String?
    /// 저장된 코스로 들어왔을 때의 제목. 있으면 [courseLabel] 대신 이걸 그대로 쓴다 —
    /// "코스 {이름}"으로 조합하면 "코스 코스 A"가 된다.
    var savedCourseName: String?
}
