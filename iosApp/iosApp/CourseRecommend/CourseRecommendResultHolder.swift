import Foundation
import SharedLogic

/// 결과 화면 상단에 그대로 보여줄 B0 설정값 — 거리, 안 가본 길 우선, 적용한 필터 수.
struct CourseRecommendSettings: Equatable {
    let distanceLabel: String
    let avoidVisitedPreferred: Bool
    let selectedFilterCount: Int
}

/// 코스 추천 결과 — 성공이면 서버 응답, 실패면 안내 문구.
enum CourseRecommendResult {
    case success(response: RoutesResponse, settings: CourseRecommendSettings)
    case failure(message: String, settings: CourseRecommendSettings)

    var settings: CourseRecommendSettings {
        switch self {
        case .success(_, let settings): return settings
        case .failure(_, let settings): return settings
        }
    }
}

/// `CourseCreateViewModel`이 `/api/v1/routes` 응답을 받으면 담아 둔다 — 복잡한 응답 객체를 화면
/// 인자로 넘기지 않고 홀더로 전달한다(Android `CourseRecommendResultHolder`와 같은 패턴).
/// 결과 화면과 코스 상세가 여기서 읽어 쓴다.
@MainActor
final class CourseRecommendResultHolder: ObservableObject {
    static let shared = CourseRecommendResultHolder()

    @Published private(set) var result: CourseRecommendResult?

    private init() {}

    func publish(_ result: CourseRecommendResult) {
        self.result = result
    }

    func clear() {
        result = nil
    }

    /// 코스 상세가 routeId로 코스를 다시 찾을 때 쓴다 — 인덱스도 같이 돌려준다("코스 A/B/C" 라벨용).
    func route(id: String) -> (route: RouteDto, index: Int)? {
        guard case .success(let response, _) = result else { return nil }
        guard let index = response.routes.firstIndex(where: { $0.routeId == id }) else { return nil }
        return (response.routes[index], index)
    }
}
