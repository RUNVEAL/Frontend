import Foundation
import SharedLogic

/// 이번 추천에 쓴 B0 설정값 전체 — Android `CourseRecommendSettings`와 같다.
///
/// 결과 화면 상단 칩에 보여주는 것만이 아니라 **그 조건으로 다시 추천받는 데도 쓴다.** 그래서
/// 표시용 라벨이 아니라 원본 값(거리 선택 상태, 필터 집합, 출발·도착 좌표)을 그대로 담는다 —
/// "5km"처럼 가공된 라벨만 들고 있으면 조건을 바꿔 다시 요청할 수가 없다.
///
/// [startLatLon]은 **이번에 실제로 쓴 좌표**다. 출발지를 안 골랐으면 그때 받은 현재 위치가
/// 박혀 있다 — 안 그러면 결과 화면에서 조건만 바꿔 다시 받을 때 출발지가 비어 버린다.
struct CourseRecommendSettings: Equatable {
    let selectedDistance: String
    let isCustomDistanceSelected: Bool
    let customDistanceIndex: Int
    let avoidVisitedPreferred: Bool
    let selectedFilters: Set<String>
    let startLatLon: LatLon?
    let endLatLon: LatLon?

    /// 칩에 쓰는 거리 라벨 — 직접입력을 골랐으면 휠 값, 아니면 프리셋 그대로(Android와 같은 계산).
    var distanceLabel: String {
        guard isCustomDistanceSelected,
              CourseCreateOptions.customDistanceValues.indices.contains(customDistanceIndex)
        else { return selectedDistance }
        return "\(CourseCreateOptions.customDistanceValues[customDistanceIndex])km"
    }

    var selectedFilterCount: Int { selectedFilters.count }

    /// 서버에 보낼 거리(m). "5km"/"12.5km" → 5000/12500 — 프리셋·커스텀 값 전부 0.5km 단위라
    /// 서버의 500m 단위 제약과 맞는다. 첫 추천과 재추천이 같은 계산을 쓰도록 여기 한 곳에 둔다.
    var distanceMeters: Int {
        let digits = distanceLabel.replacingOccurrences(of: "km", with: "")
        return Int((Double(digits) ?? 5) * 1000)
    }

    /// 출발·도착지는 그대로 두고 고를 수 있는 조건만 기본값으로 되돌린다(시트의 "초기화").
    func resetToDefaults() -> CourseRecommendSettings {
        CourseRecommendSettings(
            selectedDistance: CourseCreateOptions.defaultDistance,
            isCustomDistanceSelected: false,
            customDistanceIndex: CourseCreateOptions.defaultCustomDistanceIndex,
            avoidVisitedPreferred: true,
            selectedFilters: [],
            startLatLon: startLatLon,
            endLatLon: endLatLon
        )
    }

    /// 조건 하나만 바꾼 사본 — 시트에서 값을 고칠 때와 "순환 코스로 찾기"(도착지 비우기)에 쓴다.
    func with(
        selectedDistance: String? = nil,
        isCustomDistanceSelected: Bool? = nil,
        customDistanceIndex: Int? = nil,
        avoidVisitedPreferred: Bool? = nil,
        selectedFilters: Set<String>? = nil,
        clearEnd: Bool = false
    ) -> CourseRecommendSettings {
        CourseRecommendSettings(
            selectedDistance: selectedDistance ?? self.selectedDistance,
            isCustomDistanceSelected: isCustomDistanceSelected ?? self.isCustomDistanceSelected,
            customDistanceIndex: customDistanceIndex ?? self.customDistanceIndex,
            avoidVisitedPreferred: avoidVisitedPreferred ?? self.avoidVisitedPreferred,
            selectedFilters: selectedFilters ?? self.selectedFilters,
            startLatLon: startLatLon,
            endLatLon: clearEnd ? nil : endLatLon
        )
    }
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
