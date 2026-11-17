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
            mapSnapshotRequest: IosSnapshot.shared.request(
                points: result.routePoints,
                ownerId: result.trackId,
                variant: .resultCard,
                newRoadPaths: result.newRoadPaths
            )
        )
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
