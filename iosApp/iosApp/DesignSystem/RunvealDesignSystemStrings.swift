import Foundation

/// 디자인시스템 컴포넌트가 쓰는 정적 텍스트 — Android `core:designsystem`의
/// `res/values/strings.xml`과 1:1로 대응한다(키 이름도 그대로).
///
/// CLAUDE.md의 "정적 텍스트는 코드에 하드코딩하지 않고 string resource로 선언한다"는 규칙을 iOS에서
/// 지키는 자리다. iOS의 String Catalog(`Localizable.xcstrings`)는 키를 못 찾으면 키 문자열을 그대로
/// 화면에 그려 버려서 오타가 조용히 넘어가는데, Swift 상수는 컴파일 때 잡힌다. 다국어가 필요해지면
/// 이 상수들의 값만 `String(localized:)` 호출로 바꾸면 되고 호출부는 그대로 둘 수 있다.
enum RunvealDesignSystemStrings {
    static let runStartButtonDefaultLabel = "러닝 시작"
    static let runPauseButtonDefaultLabel = "일시정지"
    static let dongMineTag = "내 동네"
    static let percentUnit = "%"
    static let legendNew = "새로 탐험한 길"
    static let legendToday = "오늘 달린 길"
    static let legendExplored = "내가 달린 길"
    static let mapMarkerStartLabel = "출발"
    static let mapMarkerEndLabel = "도착"
    static let courseListItemNotRun = "아직 안 달림"
    static let summaryCardZoomIn = "이 동네 지도 자세히 보기"
    static let courseCardStatDistance = "거리"
    static let courseCardStatTime = "예상"
    static let courseCardStatElevation = "고도"
    static let courseCardStatNewRoad = "새 길"
    static let homeBottomPanelMyLocationContentDescription = "내 위치"
    static let locationPermissionSheetTitle = "위치 권한이 필요해요!"
    static let locationPermissionSheetDescription = "내 주변 탐험 지도를 보여주고, 달린 경로를 기록하려면 \n위치 정보가 필요해요."
    static let locationPermissionBenefitExploreRate = "현재 위치 기준 동네 탐험률 표시"
    static let locationPermissionBenefitRealtimeRoute = "러닝 중 실시간 경로 기록·안내"
    static let locationPermissionBenefitBackgroundRecord = "'항상 허용' 시 화면 꺼짐 중에도 기록"
    static let locationPermissionSheetAllow = "위치 권한 허용하기"
    static let locationPermissionSheetLater = "나중에 할게요"
    static let locationPermissionSettingsSheetTitle = "위치 권한이 필요해요!"
    static let locationPermissionSettingsSheetDescription = "현재 위치를 확인하려면 \n기기 설정에서 위치 권한을 켜 주세요."
    static let locationPermissionSettingsSheetPrimary = "설정으로 이동"
    static let locationPermissionSettingsSheetSecondary = "닫기"

    // 위치 서비스(기기 GPS) 꺼짐 — 권한과 다른 상태라 문구를 따로 둔다.
    static let locationServicesSheetTitle = "위치 서비스가 꺼져 있어요!"
    static let locationServicesSheetDescription = "현재 위치를 확인하려면 \n기기 설정에서 위치 서비스를 켜 주세요."
    static let locationServicesSheetPrimary = "위치 서비스 켜기"
    static let locationServicesSheetSecondary = "닫기"
}
