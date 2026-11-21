import Foundation
import SharedLogic
import UIKit

/// Android `CourseDetailViewModel`에 해당한다. 효과가 필요 없는 화면이라 Effect는 `NoEffect`.
@MainActor
final class CourseDetailViewModel: MviViewModel<CourseDetailIntent, CourseDetailScreenState, NoEffect> {
    /// 코스 목록(저장된 코스) 썸네일 자리 크기 — 코스 목록 72pt 컨벤션과 맞춘다.
    private static let thumbnailSize: CGFloat = 72

    /// [load]가 찾아 둔 원본 — 저장할 때 다시 쓴다(화면 상태는 포맷된 문자열뿐이라 원본이 필요하다).
    private var currentRoute: RouteDto?

    /// 새 길 길이(m) — 저장할 때도 같은 값을 써야 "내 코스" 탭 카드가 코스 상세와 같은 숫자를 보여준다.
    private var currentNewRoadMeters: Double = 0

    init() {
        super.init(initialState: .loading)
    }

    override func handleIntent(_ intent: CourseDetailIntent) async {
        switch intent {
        case .load(let source):
            switch source {
            case .recommended(let routeId): load(routeId: routeId)
            case .saved(let courseId): await loadSavedCourse(courseId: courseId)
            }
        case .saveCourse:
            await saveCourse()
        }
    }

    private func load(routeId: String) {
        guard let found = CourseRecommendResultHolder.shared.route(id: routeId) else {
            currentRoute = nil
            setState { _ in .notFound }
            return
        }
        currentRoute = found.route
        currentNewRoadMeters = IosCourseRecommend.shared.estimateNewRoadMeters(route: found.route)
        let ready = Self.ready(
            from: found.route,
            courseLabel: CourseRecommendResultViewModel.courseLabel(index: found.index),
            newRoadMeters: currentNewRoadMeters
        )
        setState { _ in .ready(ready) }
    }

    /// "내 코스"에서 들어온, 이미 저장된 코스를 로컬 `course` 테이블에서 읽는다.
    ///
    /// 다시 저장할 일이 없으므로 `currentRoute`는 비워 둔다 — "저장됨" 버튼을 눌러도
    /// [saveCourse]가 그 자리에서 빠져나온다.
    private func loadSavedCourse(courseId: String) async {
        // DB 한 줄을 읽는 동안 메인 스레드를 잡지 않는다 — Android도 `Dispatchers.IO`로 뺀다.
        let found = await Task.detached(priority: .userInitiated) {
            IosSavedCourse.shared.detail(courseId: courseId)
        }.value
        guard let detail = found else {
            currentRoute = nil
            setState { _ in .notFound }
            return
        }
        currentRoute = nil
        currentNewRoadMeters = detail.newRoadMeters

        let durationMinutes = detail.durationMinutes?.intValue
        let elevationGain = detail.elevationGainM?.intValue
        var ready = CourseDetailReady(
            // 제목은 저장해 둔 이름을 그대로 쓰므로 라벨은 비운다.
            courseLabel: "",
            distanceValue: String(format: "%.1f", detail.distanceMeters / 1000),
            distanceUnit: "km",
            // 값이 없으면 대체 텍스트 없이 공란 — 추천 코스 쪽과 같은 규칙이다.
            timeValue: durationMinutes.map { "\($0)" } ?? "",
            timeUnit: durationMinutes != nil ? "분" : "",
            elevationValue: elevationGain.map { "+\($0)" } ?? "",
            elevationUnit: elevationGain != nil ? "m" : "",
            newRoadValue: String(format: "%.1f", detail.newRoadMeters / 1000),
            newRoadUnit: "km",
            elevationProfile: detail.elevationProfile,
            routeSteps: Self.routeSteps(
                milestones: detail.milestones,
                distanceMeters: detail.distanceMeters,
                isLoop: detail.isLoop
            ),
            path: detail.path,
            isLoop: detail.isLoop
        )
        ready.isSaved = true
        ready.savedCourseName = detail.name
        setState { _ in .ready(ready) }
    }

    /// `docs/local-db/local-db-schema-design.md` §3.4 `course` 테이블에 추천 응답을 그대로 저장한다.
    /// 이미 저장했으면(중복 탭) 다시 넣지 않는다. 썸네일 렌더링도 같은 호출 안에서 끝까지 기다린다 —
    /// 따로 띄우면 저장 직후 화면을 벗어날 때 취소되면서 캐시가 안 만들어질 수 있다.
    private func saveCourse() async {
        guard let route = currentRoute, case .ready(let ready) = state, !ready.isSaved else { return }

        let courseId = await Task.detached(priority: .userInitiated) { [currentNewRoadMeters] in
            IosCourseRecommend.shared.saveCourse(
                route: route,
                courseName: CourseRecommendStrings.courseName(ready.courseLabel),
                newRoadM: currentNewRoadMeters
            )
        }.value
        guard let courseId else { return }

        await prerenderThumbnail(courseId: courseId, path: ready.path, isLoop: ready.isLoop)
        setState { state in
            guard case .ready(var next) = state else { return state }
            next.isSaved = true
            return .ready(next)
        }
    }

    /// "내 코스" 탭 목록·추천 카드가 쓸 썸네일을 저장 시점에 같이 만들어 캐시해 둔다. 실패해도 코스
    /// 저장 자체는 이미 끝났으니 조용히 무시한다 — 목록은 캐시가 없으면 플레이스홀더를 보여준다.
    private func prerenderThumbnail(courseId: String, path: [LatLon], isLoop: Bool) async {
        guard let request = IosCourseRecommend.shared.thumbnailRequest(
            courseId: courseId,
            path: path,
            isLoop: isLoop
        ) else { return }
        let sizePx = Int(Self.thumbnailSize * UIScreen.main.scale)
        _ = await CourseSnapshotRenderer.shared.imageURL(request: request, widthPx: sizePx, heightPx: sizePx)
    }

    /// [newRoadMeters]는 서버 `newRoadM` 또는 로컬 추정값(m).
    private static func ready(from route: RouteDto, courseLabel: String, newRoadMeters: Double) -> CourseDetailReady {
        let elevation = route.elevationGainM
        return CourseDetailReady(
            courseLabel: courseLabel,
            distanceValue: String(format: "%.1f", Double(route.distanceM) / 1000),
            distanceUnit: "km",
            timeValue: "\(route.durationSec / 60)",
            timeUnit: "분",
            // 값이 없으면 "-" 같은 대체 텍스트 없이 그냥 공란 — Stat이 같은 너비로 칸을 맞춘다.
            elevationValue: elevation.map { "+\($0.intValue)" } ?? "",
            elevationUnit: elevation != nil ? "m" : "",
            newRoadValue: String(format: "%.1f", newRoadMeters / 1000),
            newRoadUnit: "km",
            elevationProfile: IosCourseRecommend.shared.elevationProfileOf(route: route),
            routeSteps: routeSteps(from: route),
            path: IosCourseRecommend.shared.pathOf(route: route),
            isLoop: route.type == "LOOP"
        )
    }

    private static func routeSteps(from route: RouteDto) -> [RouteStepUiModel] {
        routeSteps(
            milestones: route.milestones,
            distanceMeters: Double(route.distanceM),
            isLoop: route.type == "LOOP"
        )
    }

    /// "출발"·중간 경유지(milestones)·"도착" 순서로 경로 정보 목록을 만든다. 추천 코스와 저장된
    /// 코스가 같은 줄을 보여야 해서 둘 다 여기를 거친다.
    private static func routeSteps(
        milestones: [MilestoneDto],
        distanceMeters: Double,
        isLoop: Bool
    ) -> [RouteStepUiModel] {
        var steps: [RouteStepUiModel] = [
            RouteStepUiModel(
                place: CourseRecommendStrings.stepStart,
                note: CourseRecommendStrings.stepStartNote,
                distanceLabel: "0.0km",
                type: .start
            )
        ]
        steps += milestones.map(routeStep(from:))
        steps.append(
            RouteStepUiModel(
                place: CourseRecommendStrings.stepEnd,
                note: isLoop ? CourseRecommendStrings.stepEndLoopNote : nil,
                distanceLabel: String(format: "%.1fkm", distanceMeters / 1000),
                type: .end
            )
        )
        return steps
    }

    /// `NEW_ROAD`는 제목을 "새 길"로 두고 서버가 주는 도로명을 그 옆에 덧붙인다 — 이름을 못 받았으면
    /// "새로 탐험하는 길"로 메운다. 나머지 `kind`는 제목이 장소명이고 옆 문구는 앱이 정한다.
    private static func routeStep(from milestone: MilestoneDto) -> RouteStepUiModel {
        let isNewRoad = milestone.kind == "NEW_ROAD"
        let placeName = milestone.name?.isEmpty == false ? milestone.name : nil

        let place: String
        if isNewRoad {
            place = CourseRecommendStrings.stepNewRoad
        } else if let placeName {
            place = placeName
        } else {
            switch milestone.kind {
            case "PARK": place = CourseRecommendStrings.stepPark
            case "RIVER": place = CourseRecommendStrings.stepRiver
            default: place = CourseRecommendStrings.stepRoad
            }
        }

        let note: String?
        switch milestone.kind {
        case "PARK": note = CourseRecommendStrings.stepParkNote
        case "RIVER": note = CourseRecommendStrings.stepRiverNote
        case "NEW_ROAD": note = placeName ?? CourseRecommendStrings.stepNewRoadNote
        default: note = nil
        }

        return RouteStepUiModel(
            place: place,
            note: note,
            distanceLabel: String(format: "%.1fkm", Double(milestone.startM) / 1000),
            type: isNewRoad ? .newRoad : .waypoint
        )
    }
}
