package com.sloth.runveal.courserecommend

import androidx.lifecycle.viewModelScope
import com.sloth.runveal.courserecommend.contract.CourseRecommendResultIntent
import com.sloth.runveal.courserecommend.contract.CourseRecommendResultState
import com.sloth.runveal.courserecommend.contract.DefaultCustomDistanceIndex
import com.sloth.runveal.courserecommend.contract.DefaultDistance
import com.sloth.runveal.courserecommend.contract.RouteCardUiModel
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.routes.RouteDto
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.launch

/** 지도 위에 뜨는 결과라 Effect는 필요 없어 [Nothing]. */
class CourseRecommendResultViewModel : MviViewModel<CourseRecommendResultIntent, CourseRecommendResultState, Nothing>(
    initialState = CourseRecommendResultState.Loading,
) {
    private val recommender = CourseRecommender()

    init {
        viewModelScope.launch {
            CourseRecommendResultHolder.state.collect { result ->
                setState { result?.let(::toReadyState) ?: CourseRecommendResultState.Loading }
            }
        }
    }

    override suspend fun handleIntent(intent: CourseRecommendResultIntent) {
        when (intent) {
            is CourseRecommendResultIntent.SelectRoute -> updateReady { copy(selectedRouteId = intent.routeId) }

            CourseRecommendResultIntent.OpenFilterSheet -> updateReady { copy(filterDraft = settings) }

            CourseRecommendResultIntent.CloseFilterSheet -> updateReady { copy(filterDraft = null) }

            is CourseRecommendResultIntent.UpdateFilterDraft -> updateReady { copy(filterDraft = intent.draft) }

            // 출발·도착지는 코스 만들기에서 고른 값이라 그대로 두고, 고를 수 있는 조건만 되돌린다.
            CourseRecommendResultIntent.ResetFilterDraft -> updateReady {
                copy(
                    filterDraft = CourseRecommendSettings(
                        selectedDistance = DefaultDistance,
                        isCustomDistanceSelected = false,
                        customDistanceIndex = DefaultCustomDistanceIndex,
                        avoidVisitedPreferred = true,
                        selectedFilters = emptySet(),
                        startLatLon = settings.startLatLon,
                        endLatLon = settings.endLatLon,
                    ),
                )
            }

            CourseRecommendResultIntent.ApplyFilterDraft -> {
                val draft = (currentState as? CourseRecommendResultState.Ready)?.filterDraft ?: return
                recommendWith(draft)
            }

            // 도착지만 비우고 나머지 조건은 그대로 — 안내 줄이 말하는 "출발지로 돌아오는 코스"다.
            CourseRecommendResultIntent.RetryAsLoopCourse -> {
                val settings = (currentState as? CourseRecommendResultState.Ready)?.settings ?: return
                recommendWith(settings.copy(endLatLon = null))
            }
        }
    }

    /**
     * 주어진 조건으로 다시 추천받는다. 결과는 [CourseRecommendResultHolder]에 publish하고, 위의
     * 구독이 그걸 받아 화면 상태를 통째로 갈아 끼운다 — 코스 만들기에서 들어왔을 때와 같은 경로다.
     *
     * 다시 받는 동안에도 조건 칩과 지도는 그대로 두고 로딩만 덮는다(화면이 비면 뒤로 간 것처럼 보인다).
     */
    private fun recommendWith(settings: CourseRecommendSettings) {
        setState { if (this is CourseRecommendResultState.Ready) copy(filterDraft = null, isReloading = true) else this }
        viewModelScope.launch {
            val result = recommender.recommend(settings, fallbackStart = null)
            // 홀더에도 올려 둔다 — 코스 상세가 여기서 routeId로 코스를 찾는다.
            CourseRecommendResultHolder.publish(result)
            // 그리고 상태를 직접 갈아 끼운다. 조건만 바꾸고 결과가 **똑같이** 오면 StateFlow가
            // 같은 값이라 보고 흘리지 않아서, 위 구독만 믿으면 로딩이 영영 안 걷힌다.
            setState { toReadyState(result) }
        }
    }

    private fun updateReady(block: CourseRecommendResultState.Ready.() -> CourseRecommendResultState.Ready) {
        setState { if (this is CourseRecommendResultState.Ready) block() else this }
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
            isLoop = type == "LOOP",
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
