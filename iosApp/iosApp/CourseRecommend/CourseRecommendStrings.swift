import Foundation

/// 코스 추천 화면들의 정적 텍스트 — Android `feature:course-recommend`의 `res/values/strings.xml` +
/// 화면 코드에 있던 한글 리터럴을 모았다.
enum CourseRecommendStrings {
    // 코스 만들기(B0)
    static let createTitle = "코스 만들기"
    static let selectStartTitle = "출발지 선택"
    static let selectEndTitle = "도착지 선택"
    static let startLabel = "출발"
    static let endLabel = "도착"
    static let currentLocation = "현위치"
    static let returnToStart = "출발지로 돌아오기"
    static let confirmStart = "이 위치를 출발지로 설정하기"
    static let confirmEnd = "이 위치를 도착지로 설정하기"
    static let hintPickStart = "지도를 탭해 출발지를 선택하세요"
    static let hintPickEnd = "지도를 탭해 도착지를 선택하세요"
    static let pickLocation = "위치를 선택하세요"
    /// 좌표→실제 주소/동 이름 변환은 서버·앱 모두에 아직 없다(API.md §1 — 제3자 지오코딩 API를 안 씀).
    static let distanceNotImplemented = "거리 표시 미구현"
    static let recommendAction = "경로 추천 받기"
    static let avoidVisitedPreferred = "안 가본 길 우선"

    // 조건 설정 시트
    static let filterSheetTitle = "코스 조건 설정하기"
    static let filterSheetReset = "초기화"
    static let filterSheetApply = "코스 조건 적용하기"
    static let desiredDistance = "원하는 거리"
    static let customDistance = "직접입력"
    static let avoidVisitedTitle = "안 가본 길 우선 추천"
    static let avoidVisitedDescription = "안 가본 길을 우선으로 추천해요"
    static let runningFilter = "러닝 필터"

    // 추천 결과(B1)
    static let resultTitle = "추천 코스"
    static let resultEmptyAction = "다시 설정하기"
    static func courseName(_ label: String) -> String { "코스 \(label)" }
    static let tagPark = "공원"
    static let tagRiverside = "하천길"
    static let tagGentleSlope = "완만한 길"
    static let tagFlat = "평탄한 길"
    static let resultEmptyDefault = "추천받은 코스가 없어요."
    static let resultEmptyNoRoute = "조건에 맞는 코스를 못 찾았어요. 거리나 필터를 바꿔보세요."
    static func resultEmptyTooFar(_ km: String) -> String {
        "목표 거리로는 코스를 못 찾았어요. 최소 \(km)km는 필요해요."
    }
    static let recommendFailed = "코스를 추천받지 못했어요"

    // 코스 상세(B2)
    static let detailNotFound = "코스를 더 이상 찾을 수 없어요"
    static let routeInfoTitle = "경로 정보"
    static let saveAction = "코스 저장"
    static let savedLabel = "저장됨"
    static let startRunningAction = "이 코스로 러닝 시작"
    static let stepStart = "출발"
    static let stepStartNote = "현재 위치"
    static let stepEnd = "도착"
    static let stepEndLoopNote = "출발지"
    static let stepNewRoad = "새 길"
    static let stepNewRoadNote = "새로 탐험하는 길"
    static let stepPark = "공원"
    static let stepParkNote = "공원 지나가기"
    static let stepRiver = "하천길"
    static let stepRiverNote = "하천길 따라가기"
    static let stepRoad = "도로"

    /// `RouteDto.tags`는 서버 코드로 와서 한글 라벨로 바꿔 보여준다.
    static func tagLabel(_ code: String) -> String {
        switch code {
        case "PARK": return tagPark
        case "RIVERSIDE": return tagRiverside
        case "GENTLE_SLOPE": return tagGentleSlope
        case "FLAT": return tagFlat
        default: return code
        }
    }
}
