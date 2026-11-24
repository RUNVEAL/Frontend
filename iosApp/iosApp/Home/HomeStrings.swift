import Foundation

/// 홈 셸의 정적 텍스트 — Android `feature:home`의 `res/values/strings.xml`과 1:1 대응.
enum HomeStrings {
    static let locationPermissionOffBannerMessage = "위치 권한이 꺼져 있어요"
    static let locationPermissionOffBannerAction = "허용하기"
    static let locationServicesOffBannerMessage = "위치 서비스가 꺼져 있어요"
    static let locationServicesOffBannerAction = "위치 켜기"

    // 서버 데이터 안내 배너 — 뭘 못 받았는지에 따라 문구가 다르다. 동 이름만 실패했으면 지도는
    // 제대로 보이는데 "지도 데이터를 못 받았다"는 틀린 안내가 된다(Android와 같은 분기).
    static let serverDataErrorBannerMessageMap = "지도 데이터를 받지 못했어요"
    static let serverDataErrorBannerMessageDong = "현재 위치 정보를 받지 못했어요"
    static let serverDataErrorBannerMessageRetrying = "데이터를 다시 받고 있어요"
    static let serverDataErrorBannerAction = "다시 시도"
    static let recordingDiscardedToast = "100m·1분을 넘겨야 기록으로 저장돼요"

    static let tabHome = "홈"
    static let tabCourse = "내 코스"
    static let tabRecord = "기록"
    static let tabMy = "MY"
}
