import Foundation

/// MY 탭의 정적 텍스트 — Android `feature:tab-mypage`의 `res/values/strings.xml`과 1:1 대응.
enum MyStrings {
    static let title = "MY"

    static let statExploredDongLabel = "탐험한 동"
    static let statExploredDongUnit = "개"
    static let statTotalDistanceLabel = "누적 거리"
    static let statTotalDistanceUnit = "km"

    static let sectionSettings = "설정"
    static let menuLocationPermission = "위치 권한 설정"
    static let menuLocationPermissionGranted = "허용됨"
    static let menuLocationPermissionDenied = "허용 안 됨"

    static let termsPolicyTitle = "약관 및 정책"
    static let termsPolicyService = "서비스 이용약관"
    static let termsPolicyPrivacy = "개인정보 처리방침"
    static let termsPolicyOpenSource = "오픈소스 라이선스"
    static let termsPolicyAppVersion = "앱 버전 정보"

    static let ossMapSection = "지도"
    static let ossMapData = "지도 데이터 © OpenStreetMap contributors"
    static let ossMapDataLicense = "ODbL 1.0"
    static let ossMapTiles = "지도 타일 OpenFreeMap · OpenMapTiles"
    static let ossMapTilesLicense = "BSD 3-Clause"
    static let ossMapDong = "행정동 경계 행정안전부 · vuski/admdongkor"
    static let ossMapDongLicense = "CC BY 4.0 · 공공누리 제1유형"
    static let ossLibrarySection = "라이브러리"
}
