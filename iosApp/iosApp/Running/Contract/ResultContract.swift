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
    /**
     저장 이미지 우측 상단에 찍는 날짜("2026.10.25").

     화면에 쓰는 [dateLabel]("10월 25일 일요일 저녁 러닝")과 따로 두는 이유: 이미지에는 한 줄에
     들어가는 짧은 형식이 필요하고, 기준도 **저장한 날이 아니라 달린 날**이라 러닝 시작 시각에서
     뽑는다(나중에 결과를 다시 열어도 그날 날짜가 찍힌다).
     */
    let shareDateLabel: String
    /**
     저장 이미지가 경로를 **벡터로 직접** 그릴 때 쓰는 점들([RouteVectorMap]).

     [mapSnapshotRequest]와 같은 경로지만 GeoJSON이 아니라 좌표 그대로다 — 저장 이미지는 지도 타일을
     깔지 않아서(네트워크·저작권 표기) 점을 직접 받아 선으로 그린다.
     */
    let routePaths: [[LatLon]]
    /// [routePaths] 위에 금색으로 덧그릴 "이번에 처음 간" 구간. 매칭이 실패하면 비어 있다.
    let newRoadPaths: [[LatLon]]
}
