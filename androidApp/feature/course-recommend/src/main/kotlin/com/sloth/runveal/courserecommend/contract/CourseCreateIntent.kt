package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.ui.mvi.UiIntent

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
    /** 출발지를 한 번도 안 골랐을 때 쓸 현재 위치 — 화면이 미리 받아서 같이 넘긴다(권한/위치 서비스는
     * ViewModel이 몰라도 되게). 못 받았으면 null. */
    data class RecommendClicked(val currentLocation: LatLon?) : CourseCreateIntent
}
