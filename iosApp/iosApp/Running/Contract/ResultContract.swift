import Foundation
import SharedLogic

struct ResultDongStat: Identifiable, Equatable {
    var id: String { name }
    let name: String
    let beforePercent: Int
    let afterPercent: Int
}

/// `TrackResultHolder`가 비어 있는 동안은 로딩 — 최종 매칭(§6)이 끝나야 준비된다.
enum ResultScreenState: UiState {
    case loading
    case ready(ResultReady)
}

struct ResultReady {
    let dateLabel: String
    let newRoadLabel: String
    let dongStats: [ResultDongStat]
    let distanceLabel: String
    let elapsedLabel: String
    let paceLabel: String
    /// 경로 점이 2개 미만(제자리 종료 등)이면 nil — 지도 카드가 그때는 플레이스홀더만 보여준다.
    let mapSnapshotRequest: SnapshotRequest?
}
