package com.swyp.runveal.courserecommend

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.courserecommend.contract.CourseRecommendResultIntent
import com.swyp.runveal.courserecommend.contract.CourseRecommendResultState
import com.swyp.runveal.courserecommend.contract.RouteCardUiModel
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.network.routes.RouteDto
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.launch

/** 지도 위에 뜨는 결과라 Effect는 필요 없어 [Nothing]. */
class CourseRecommendResultViewModel : MviViewModel<CourseRecommendResultIntent, CourseRecommendResultState, Nothing>(
    initialState = CourseRecommendResultState.Loading,
) {
    init {
        viewModelScope.launch {
            CourseRecommendResultHolder.state.collect { result ->
                setState { result?.let(::toReadyState) ?: CourseRecommendResultState.Loading }
            }
        }
    }

    override suspend fun handleIntent(intent: CourseRecommendResultIntent) {
        when (intent) {
            is CourseRecommendResultIntent.SelectRoute -> setState {
                if (this is CourseRecommendResultState.Ready) copy(selectedRouteId = intent.routeId) else this
            }
        }
    }

    private fun toReadyState(result: CourseRecommendResult): CourseRecommendResultState.Ready =
        when (result) {
            is CourseRecommendResult.Success -> {
                val routes = result.response.routes.mapIndexed { index, route -> route.toUiModel(index) }
                CourseRecommendResultState.Ready(
                    routes = routes,
                    emptyMessage = if (routes.isEmpty()) {
                        emptyMessageFor(result.response.warnings, result.response.shortestDistanceM)
                    } else {
                        null
                    },
                    selectedRouteId = routes.firstOrNull()?.routeId,
                    settings = result.settings,
                )
            }
            is CourseRecommendResult.Failure -> CourseRecommendResultState.Ready(
                routes = emptyList(),
                emptyMessage = result.message,
                settings = result.settings,
            )
        }

    private fun RouteDto.toUiModel(index: Int): RouteCardUiModel {
        val elevation = elevationGainM
        val newRoad = newRoadM
        return RouteCardUiModel(
            routeId = routeId,
            courseLabel = ('A' + index).toString(),
            tags = tags.take(2),
            distanceValue = "%.1f".format(distanceM / 1000.0),
            distanceUnit = "km",
            timeValue = (durationSec / 60).toString(),
            timeUnit = "분",
            // 값이 없으면 "-" 같은 대체 텍스트 없이 그냥 공란 — RunvealCourseCard가 weight로 칸 폭을 맞춘다.
            elevationValue = elevation?.let { "+$it" } ?: "",
            elevationUnit = if (elevation != null) "m" else "",
            newRoadValue = newRoad?.let { "%.1f".format(it / 1000.0) } ?: "",
            newRoadUnit = if (newRoad != null) "km" else "",
            // GeoJSON 좌표 순서라 [lng, lat](§5.2) — LatLon은 반대 순서.
            path = path.map { LatLon(lat = it[1], lon = it[0]) },
        )
    }

    /** §6.3 "결과 0개" 케이스 — `warnings`로 안내 문구를 고른다. */
    private fun emptyMessageFor(warnings: List<String>, shortestDistanceM: Long?): String = when {
        "DESTINATION_FARTHER_THAN_DISTANCE" in warnings && shortestDistanceM != null ->
            "목표 거리로는 코스를 못 찾았어요. 최소 %.1fkm는 필요해요.".format(shortestDistanceM / 1000.0)
        "NO_ROUTE_FOUND" in warnings -> "조건에 맞는 코스를 못 찾았어요. 거리나 필터를 바꿔보세요."
        else -> "추천받은 코스가 없어요."
    }
}
