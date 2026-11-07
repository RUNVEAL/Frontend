package com.swyp.runveal.courserecommend

import com.swyp.runveal.courserecommend.contract.CourseCreateIntent
import com.swyp.runveal.courserecommend.contract.CourseCreateState
import com.swyp.runveal.courserecommend.contract.DefaultCustomDistanceIndex
import com.swyp.runveal.courserecommend.contract.DefaultDistance
import com.swyp.runveal.ui.mvi.MviViewModel

/** 효과가 필요 없는 화면이라 Effect 타입 인자는 [Nothing]. */
class CourseCreateViewModel : MviViewModel<CourseCreateIntent, CourseCreateState, Nothing>(
    initialState = CourseCreateState(),
) {
    override suspend fun handleIntent(intent: CourseCreateIntent) {
        when (intent) {
            CourseCreateIntent.OpenFilterSheet -> setState { copy(showFilterSheet = true) }
            CourseCreateIntent.CloseFilterSheet -> setState { copy(showFilterSheet = false) }

            is CourseCreateIntent.SelectPresetDistance -> {
                val distance = intent.distance
                setState { copy(selectedDistance = distance, isCustomDistanceSelected = false) }
            }

            CourseCreateIntent.SelectCustomDistanceChip -> setState { copy(isCustomDistanceSelected = true) }

            is CourseCreateIntent.ChangeCustomDistanceIndex -> {
                val index = intent.index
                setState { copy(customDistanceIndex = index) }
            }

            is CourseCreateIntent.SetAvoidVisitedPreferred -> {
                val preferred = intent.preferred
                setState { copy(avoidVisitedPreferred = preferred) }
            }

            is CourseCreateIntent.ToggleFilter -> {
                val filter = intent.filter
                setState {
                    val filters = if (filter in selectedFilters) selectedFilters - filter else selectedFilters + filter
                    copy(selectedFilters = filters)
                }
            }

            CourseCreateIntent.ResetFilterSheet -> setState {
                copy(
                    selectedDistance = DefaultDistance,
                    isCustomDistanceSelected = false,
                    customDistanceIndex = DefaultCustomDistanceIndex,
                    avoidVisitedPreferred = true,
                    selectedFilters = emptySet(),
                )
            }

            CourseCreateIntent.ApplyFilterSheet -> setState { copy(showFilterSheet = false) }

            CourseCreateIntent.RecommendClicked -> {
                // TODO: 실제 경로 추천 연동
            }
        }
    }
}
