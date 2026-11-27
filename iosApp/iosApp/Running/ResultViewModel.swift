import Foundation
import SharedLogic

/// 사용자가 보낼 의도가 없는 화면(버튼은 전부 네비게이션/TODO) — Intent/Effect 타입 인자로
/// `NoIntent`/`NoEffect`를 쓴다. Android는 둘 다 `Nothing`.
enum NoIntent: UiIntent {}

@MainActor
final class ResultViewModel: MviViewModel<NoIntent, ResultScreenState, NoEffect> {
    private var subscription: FlowSubscription?

    init() {
        super.init(initialState: .loading)
        subscription = RunvealFlowWatchers.shared.watchTrackResult { [weak self] result in
            guard let self else { return }
            self.setState { _ in
                guard let result else { return .loading }
                return .ready(Self.ready(from: result))
            }
        }
    }

    deinit {
        subscription?.cancel()
    }

    func onDisappear() {
        subscription?.cancel()
        subscription = nil
        cancelRunningTasks()
    }

    override func handleIntent(_ intent: NoIntent) async {}

    private static func ready(from result: TrackResult) -> ResultReady {
        ResultReady(
            dateLabel: dateLabel(epochMs: result.startedAtEpochMs),
            newRoadLabel: RunFormat.kilometers(result.newRoadMeters, fractionDigits: 1),
            dongStats: result.dongStats.map {
                ResultDongStat(
                    name: $0.name,
                    beforePercent: Int($0.beforePercent),
                    afterPercent: Int($0.afterPercent)
                )
            },
            distanceLabel: RunFormat.kilometers(result.distanceMeters),
            elapsedLabel: RunFormat.elapsed(result.elapsedMs),
            paceLabel: RunFormat.pace(elapsedMs: result.elapsedMs, distanceMeters: result.distanceMeters),
            // 매칭 경로는 GPS와 동떨어진 부분을 잘라낸 조각 목록이라 조각 그대로 넘긴다 —
            // 이어 붙이면 잘라낸 구간이 직선으로 되살아난다.
            mapSnapshotRequest: IosSnapshot.shared.requestForPaths(
                paths: result.routePaths,
                ownerId: result.trackId,
                variant: .resultCard,
                newRoadPaths: result.newRoadPaths
            ),
            shareDateLabel: shareDateLabel(epochMs: result.startedAtEpochMs),
            // 저장 이미지는 지도 타일 없이 선만 그려서 좌표가 그대로 필요하다. Kotlin의
            // `List<List<LatLon>>`은 조각 단위로 받아 내려야 Swift 쪽 타입이 분명해진다.
            routePaths: Self.segments(result.routePaths),
            newRoadPaths: Self.segments(result.newRoadPaths)
        )
    }

    /// 저장 이미지용 날짜 — "2026.10.25". 자릿수가 고정이라 길이가 들쭉날쭉하지 않다
    /// (Android `ResultViewModel.formatShareDate`와 같은 형식).
    private static func shareDateLabel(epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d.%02d.%02d", parts.year ?? 0, parts.month ?? 1, parts.day ?? 1)
    }

    /// 경로 조각 목록을 Swift 배열로 내린다. 조각을 이어 붙이지 않는다 — 매칭이 잘라낸 구간이
    /// 직선으로 되살아난다.
    private static func segments(_ paths: Any) -> [[LatLon]] {
        guard let raw = paths as? [Any] else { return [] }
        return raw.compactMap { $0 as? [LatLon] }
    }

    /// "10월 25일 일요일 저녁 러닝" — Android `ResultViewModel.formatDate`와 같은 규칙.
    private static func dateLabel(epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        let parts = calendar.dateComponents([.month, .day, .hour, .weekday], from: date)
        let month = parts.month ?? 1
        let day = parts.day ?? 1
        let hour = parts.hour ?? 0

        // Calendar.weekday: 일요일 = 1.
        let dayNames = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"]
        let dayOfWeek = dayNames[((parts.weekday ?? 1) - 1) % 7]

        let timeOfDay: String
        switch hour {
        case 5...10: timeOfDay = "아침"
        case 11...13: timeOfDay = "점심"
        case 14...17: timeOfDay = "오후"
        case 18...21: timeOfDay = "저녁"
        default: timeOfDay = "밤"
        }
        return "\(month)월 \(day)일 \(dayOfWeek) \(timeOfDay) 러닝"
    }
}
