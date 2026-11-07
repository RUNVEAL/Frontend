package com.swyp.runveal.courserecommend.contract

import com.swyp.runveal.ui.mvi.UiState

internal val DistanceOptions = listOf("3km", "5km", "7km", "10km")
internal const val DefaultDistance = "5km"
internal val FilterOptions = listOf("신호/횡단보도 적은 길", "완만한 길", "공원 지나가기", "하천길 따라가기")

/** 1km ~ 42km, 0.5km 단위. 인덱스 22 = "12" (휠 피커 기본값). */
internal val CustomDistanceValues = (2..84).map { step ->
    val km = step / 2.0
    if (km % 1.0 == 0.0) km.toInt().toString() else km.toString()
}
internal const val DefaultCustomDistanceIndex = 22

/** 상단 앱바 제목을 바꾸는 출발/도착 선택 모드. 선택 중이 아니면 [None]. */
enum class LocationSelectionTarget { None, Start, End }

data class CourseCreateState(
    val startLocation: String = "현위치",
    val endLocation: String = "출발지로 돌아오기",
    val endLocationIsDefault: Boolean = true,
    val locationSelectionTarget: LocationSelectionTarget = LocationSelectionTarget.None,
    // TODO: 지도 핀 연동 전까지의 임시 값 — 실제로는 선택 중인 지도 중심 지점의 역지오코딩 결과/거리로 갱신돼야 한다.
    val selectedPlaceName: String = "성동구 성수 1가 1동",
    val selectedPlaceDistance: String = "0.1km",
    val distanceOptions: List<String> = DistanceOptions,
    val selectedDistance: String = DefaultDistance,
    val isCustomDistanceSelected: Boolean = false,
    val customDistanceValues: List<String> = CustomDistanceValues,
    val customDistanceIndex: Int = DefaultCustomDistanceIndex,
    val avoidVisitedPreferred: Boolean = true,
    val filterOptions: List<String> = FilterOptions,
    val selectedFilters: Set<String> = emptySet(),
    val showFilterSheet: Boolean = false,
) : UiState {
    val distanceLabel: String
        get() = if (isCustomDistanceSelected) "${customDistanceValues[customDistanceIndex]}km" else selectedDistance

    val topBarTitle: String
        get() = when (locationSelectionTarget) {
            LocationSelectionTarget.None -> "코스 만들기"
            LocationSelectionTarget.Start -> "출발지 선택"
            LocationSelectionTarget.End -> "도착지 선택"
        }

    val locationConfirmLabel: String
        get() = when (locationSelectionTarget) {
            LocationSelectionTarget.None -> ""
            LocationSelectionTarget.Start -> "이 위치를 출발지로 설정하기"
            LocationSelectionTarget.End -> "이 위치를 도착지로 설정하기"
        }

    val locationHintMessage: String
        get() = when (locationSelectionTarget) {
            LocationSelectionTarget.None -> ""
            LocationSelectionTarget.Start -> "지도를 탭해 출발지를 선택하세요"
            LocationSelectionTarget.End -> "지도를 탭해 도착지를 선택하세요"
        }
}
