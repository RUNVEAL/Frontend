import Foundation
import SharedLogic

enum RecordingIntent: UiIntent {
    case startClicked
    case pauseClicked
    case resumeClicked
    /// "종료하기" — 바로 끝내지 않고 확인 다이얼로그를 띄운다.
    case stopRequested
    /// 다이얼로그를 닫고 계속 달린다.
    case stopDismissed
    /// 다이얼로그에서 끝내기를 눌렀다 — 여기서부터 실제로 멈춘다.
    case stopClicked
}

/// 위치 수신은 `RunvealRunTracker`(CoreLocation)가 맡아 ViewModel이 직접 부르지 않는다 —
/// 화면이 `.collectEffect`로 받아 실행한다. Android `RecordingEffect`와 같은 구조.
enum RecordingEffect: UiEffect {
    case startTracking
    case pauseTracking
    case resumeTracking
    /// [saveRecord]가 false면 너무 짧아 버리는 러닝 — 기록도 저장하지 않고, 화면도 결과 화면 대신
    /// 홈으로 돌아간다(`RecordingSavePolicy`).
    case stopTracking(saveRecord: Bool)
}

/// 화면이 보여주는 기록 단계 — sharedLogic `RecordingPhase`(IDLE/RECORDING/PAUSED/FINISHED)와 달리
/// 이 화면은 FINISHED를 그리지 않는다(종료하면 바로 다른 화면으로 넘어간다).
enum RecordingScreenPhase {
    case idle
    case recording
    case paused
}

/// 라벨은 이미 포맷된 문자열로 들고 있는다(화면은 그대로 쓰기만 한다). [distanceLabel]은 단위(km)
/// 없이 숫자만 — 디자인이 숫자/단위를 다른 스타일로 그리기 때문이다.
struct RecordingScreenState: UiState {
    var phase: RecordingScreenPhase = .idle
    var elapsedLabel: String = "00:00"
    var distanceLabel: String = "0.00"
    var paceLabel: String = "0'00″"
    /// 이번 러닝에서 처음 가보는 길만 합친 거리(km, 단위 없이 숫자만) — 총 거리와 달리 이미 가본
    /// 구간은 빠진다. 지도가 금색으로 칠하는 선과 같은 분류 결과를 쓴다(`LiveNewRoadHolder`).
    var newRoadLabel: String = "0.00"
    /// 떠 있는 종료 확인 다이얼로그. nil이면 안 떠 있다.
    var finishDialog: RecordingFinishDialog?
    /// "이 코스로 러닝 시작"으로 넘어온 코스 경로.
    var selectedCoursePath: [LatLon]?
    /// 선택한 코스가 순환이면 출발·도착이 같은 자리라 도착 핀을 생략한다(코스 상세와 같은 규칙).
    var selectedCourseIsLoop: Bool = false
    /// 저장된 코스로 시작한 경우 그 코스의 id — 기록이 저장될 때 "달림"으로 적는 데 쓴다.
    /// 자유 러닝이나 저장 전 추천 코스는 nil이다.
    var selectedCourseId: String?
}

/// 종료 확인 다이얼로그에 넘길 것 — [willSaveRecord]가 false면 100m/1분을 못 채워 **저장되지 않는**
/// 러닝이라, 문구와 버튼을 "저장 없이 종료"로 바꿔 끝내기 전에 알린다(`RecordingSavePolicy`).
struct RecordingFinishDialog {
    let willSaveRecord: Bool
}

/// "이 코스로 러닝 시작"일 때만 패널에 더 보여주는 코스 진행 정보.
struct RecordingCourseProgress: Equatable {
    let remainingDistanceLabel: String
    let progressPercent: Int
}
