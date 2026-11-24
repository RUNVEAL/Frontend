import Foundation

/// 러닝 기록·결과 화면의 정적 텍스트 — Android `feature:running`의 `res/values/strings.xml`과 1:1 대응.
enum RunningStrings {
    static let recordingElapsedLabel = "경과 시간"
    static let recordingPaceLabel = "평균 페이스"
    static let recordingDistanceUnit = "km"
    static let recordingStopButton = "종료하기"
    static let recordingResumeButton = "재개"
    static let recordingFinishDialogTitle = "러닝을 끝낼까요?"
    static let recordingFinishDialogDescription = "지금까지 달린 기록이 저장되고 탐험 지도에 반영돼요."
    static let recordingFinishDialogConfirm = "끝내기"
    static let recordingFinishDialogCancel = "취소"
    static let recordingFinishDialogUnsavedTitle = "러닝 기록이 너무 짧아요"
    static let recordingFinishDialogUnsavedDescription = "100m 또는 1분 미만은 저장되지 않아요."
    static let recordingFinishDialogUnsavedConfirm = "저장 없이 종료"
    static let recordingFinishDialogUnsavedCancel = "계속 달리기"
    static let recordingLegendTodayLabel = "오늘 달린 길"
    static let recordingLegendNewLabel = "새로 탐험한 길"
    static let recordingLegendCourseLabel = "추천 코스"
    static func recordingCourseRemainingDistance(_ km: String) -> String { "남은 거리 \(km)km" }
    static let recordingCourseProgressLabel = "코스 진행"
    static let recordingCourseProgressUnit = "%"
    static let recordingLocateAccessibilityLabel = "내 위치"
    static func recordingNewDistance(_ km: String) -> String { "+\(km)km" }

    static let resultTitle = "러닝 완료!"
    static func resultNewRoadValue(_ km: String) -> String { "+\(km)" }
    static func resultDongRateLabel(_ name: String) -> String { "\(name) 탐험률" }
    static let resultRateArrow = "→"
    static func resultRatePercent(_ percent: Int) -> String { "\(percent)%" }
    static let resultDistanceLabel = "거리"
    static let resultElapsedLabel = "시간"
    static let resultDetailButton = "기록 상세보기"
    static let resultSaveImageButton = "이미지 저장하기"
    static let resultCloseAccessibilityLabel = "닫기"
    static let resultMapAccessibilityLabel = "이번 러닝 경로 지도"
    /// 기록 상세·이미지 저장은 아직 없다 — Android도 같은 문구의 안내만 띄운다.
    static let notReady = "아직 개발 중입니다"

    // 일시정지 배너 — 지도 위에 띄워 기록이 멈춘 걸 알린다(시안 C0-1).
    static let pausedBannerTitle = "일시정지 중"
    static let pausedBannerDescription = "러닝 기록이 멈췄어요"
}

/// 경과 시간·페이스 포맷 — 기록 화면과 결과 화면, 기록 탭이 같은 공식을 써야 한다
/// (결과에서 6'05″로 보이는 페이스가 목록에서 다르면 안 된다).
enum RunFormat {
    /// "MM:SS" — 60분을 넘어도 분으로만 센다(Android와 같다).
    static func elapsed(_ elapsedMs: Int64) -> String {
        let totalSeconds = elapsedMs / 1000
        return String(format: "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    /// "6'02″" 형태 — km당 분'초″.
    static func pace(elapsedMs: Int64, distanceMeters: Double) -> String {
        guard distanceMeters > 0 else { return "0'00″" }
        let paceSecondsPerKm = (Double(elapsedMs) / 1000) / (distanceMeters / 1000)
        let minutes = Int(paceSecondsPerKm / 60)
        let seconds = Int(paceSecondsPerKm.truncatingRemainder(dividingBy: 60))
        return "\(minutes)'" + String(format: "%02d", seconds) + "″"
    }

    static func kilometers(_ meters: Double, fractionDigits: Int = 2) -> String {
        String(format: "%.\(fractionDigits)f", meters / 1000)
    }
}
