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

data class CourseCreateState(
    val startLocation: String = "현위치",
    val endLocation: String = "출발지로 돌아오기",
    val endLocationIsDefault: Boolean = true,
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
}
