package com.swyp.runveal.courserecommend.contract

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.ui.mvi.UiIntent

sealed interface CourseCreateIntent : UiIntent {
    data object StartLocationClicked : CourseCreateIntent
    data object EndLocationClicked : CourseCreateIntent
    data object CancelLocationSelection : CourseCreateIntent
    data object ConfirmLocationSelection : CourseCreateIntent
    /** 출발/도착 선택 모드에서 지도를 탭했을 때 — 확정 전까지의 임시 좌표. */
    data class MapLocationPicked(val location: LatLon) : CourseCreateIntent
    data object OpenFilterSheet : CourseCreateIntent
    data object CloseFilterSheet : CourseCreateIntent
    data class SelectPresetDistance(val distance: String) : CourseCreateIntent
    data object SelectCustomDistanceChip : CourseCreateIntent
    data class ChangeCustomDistanceIndex(val index: Int) : CourseCreateIntent
    data class SetAvoidVisitedPreferred(val preferred: Boolean) : CourseCreateIntent
    data class ToggleFilter(val filter: String) : CourseCreateIntent
    data object ResetFilterSheet : CourseCreateIntent
    data object ApplyFilterSheet : CourseCreateIntent
    data object RecommendClicked : CourseCreateIntent
}
