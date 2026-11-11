package com.swyp.runveal.courserecommend

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.courserecommend.contract.CourseRecommendResultState
import com.swyp.runveal.courserecommend.contract.RouteCardUiModel
import com.swyp.runveal.network.routes.RouteDto
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.launch

/** 버튼 없는 읽기 전용 화면이라 Intent/Effect 둘 다 [Nothing]. */
class CourseRecommendResultViewModel : MviViewModel<Nothing, CourseRecommendResultState, Nothing>(
    initialState = CourseRecommendResultState.Loading,
) {
    init {
        viewModelScope.launch {
            CourseRecommendResultHolder.state.collect { result ->
                setState { result?.let(::toReadyState) ?: CourseRecommendResultState.Loading }
            }
        }
    }

    override suspend fun handleIntent(intent: Nothing) {}

    private fun toReadyState(result: CourseRecommendResult): CourseRecommendResultState.Ready =
        when (result) {
            is CourseRecommendResult.Success -> CourseRecommendResultState.Ready(
                routes = result.response.routes.map { it.toUiModel() },
                emptyMessage = if (result.response.routes.isEmpty()) {
                    emptyMessageFor(result.response.warnings, result.response.shortestDistanceM)
                } else {
                    null
                },
            )
            is CourseRecommendResult.Failure -> CourseRecommendResultState.Ready(
                routes = emptyList(),
                emptyMessage = result.message,
            )
        }

    private fun RouteDto.toUiModel(): RouteCardUiModel = RouteCardUiModel(
        routeId = routeId,
        typeLabel = if (type == "LOOP") "순환" else "편도",
        distanceLabel = "%.1fkm".format(distanceM / 1000.0),
        durationLabel = formatDuration(durationSec),
        elevationGainLabel = elevationGainM?.let { "상승 ${it}m" },
        newRoadLabel = newRoadM?.let { "새 길 +%.1fkm".format(it / 1000.0) },
        tags = tags,
        milestoneNames = milestones.mapNotNull { it.name }.distinct(),
    )

    /** "6분"/"1시간 12분" — [docs/api/API.md] §3.3.2 `durationSec`. */
    private fun formatDuration(durationSec: Int): String {
        val minutes = durationSec / 60
        return if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
    }

    /** §6.3 "결과 0개" 케이스 — `warnings`로 안내 문구를 고른다. */
    private fun emptyMessageFor(warnings: List<String>, shortestDistanceM: Long?): String = when {
        "DESTINATION_FARTHER_THAN_DISTANCE" in warnings && shortestDistanceM != null ->
            "목표 거리로는 코스를 못 찾았어요. 최소 %.1fkm는 필요해요.".format(shortestDistanceM / 1000.0)
        "NO_ROUTE_FOUND" in warnings -> "조건에 맞는 코스를 못 찾았어요. 거리나 필터를 바꿔보세요."
        else -> "추천받은 코스가 없어요."
    }
}
