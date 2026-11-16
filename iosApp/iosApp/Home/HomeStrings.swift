import Foundation

/// 홈 셸의 정적 텍스트 — Android `feature:home`의 `res/values/strings.xml`과 1:1 대응.
enum HomeStrings {
    static let locationPermissionOffBannerMessage = "위치 권한이 꺼져 있어요"
    static let locationPermissionOffBannerAction = "허용하기"
    static let locationServicesOffBannerMessage = "위치 서비스가 꺼져 있어요"
    static let locationServicesOffBannerAction = "위치 켜기"
    static let recordingDiscardedToast = "100m·1분을 넘겨야 기록으로 저장돼요"

    static let tabHome = "홈"
    static let tabCourse = "내 코스"
    static let tabRecord = "기록"
    static let tabMy = "MY"
}
