package com.swyp.runveal.courserecommend

import android.util.Log
import com.swyp.runveal.courserecommend.contract.CourseCreateEffect
import com.swyp.runveal.courserecommend.contract.CourseCreateIntent
import com.swyp.runveal.courserecommend.contract.CourseCreateState
import com.swyp.runveal.courserecommend.contract.DefaultCustomDistanceIndex
import com.swyp.runveal.courserecommend.contract.DefaultDistance
import com.swyp.runveal.courserecommend.contract.LocationSelectionTarget
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.network.routes.LatLng
import com.swyp.runveal.network.routes.RoutesApi
import com.swyp.runveal.network.routes.RoutesRequest
import com.swyp.runveal.ui.mvi.MviViewModel

private const val TAG = "CourseRecommendDebug"

/** TEMP — 지도 핀으로 실제 출발지를 고르는 기능이 아직 없어서 쓰는 임시 좌표(성수동 인근).
 * [com.swyp.runveal.courserecommend.contract.CourseCreateState]의 `selectedPlaceName` 기본값과
 * 맞춘 자리다 — 지도 연동이 들어오면 지워진다. */
private val TEMP_START_LOCATION = LatLng(lat = 37.5445, lng = 127.0557)

class CourseCreateViewModel : MviViewModel<CourseCreateIntent, CourseCreateState, CourseCreateEffect>(
    initialState = CourseCreateState(),
) {
    private val routesApi = RoutesApi()

    override suspend fun handleIntent(intent: CourseCreateIntent) {
        when (intent) {
            CourseCreateIntent.StartLocationClicked ->
                setState { copy(locationSelectionTarget = LocationSelectionTarget.Start) }

            CourseCreateIntent.EndLocationClicked ->
                setState { copy(locationSelectionTarget = LocationSelectionTarget.End) }

            CourseCreateIntent.CancelLocationSelection ->
                setState { copy(locationSelectionTarget = LocationSelectionTarget.None) }

            CourseCreateIntent.ConfirmLocationSelection -> setState {
                when (locationSelectionTarget) {
                    LocationSelectionTarget.Start -> copy(
                        startLocation = selectedPlaceName,
                        locationSelectionTarget = LocationSelectionTarget.None,
                    )
                    LocationSelectionTarget.End -> copy(
                        endLocation = selectedPlaceName,
                        endLocationIsDefault = false,
                        locationSelectionTarget = LocationSelectionTarget.None,
                    )
                    LocationSelectionTarget.None -> this
                }
            }

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

            CourseCreateIntent.RecommendClicked -> recommendRoutes()
        }
    }

    /** `docs/api/API.md` §3.3 — B0 설정값으로 `/api/v1/routes`를 호출하고, 성공·실패 모두
     * [CourseRecommendResultHolder]에 publish한 뒤 결과 화면으로 넘어간다. `selectedFilters`는 화면
     * 라벨(한글)이라 서버 필터 코드(`PARK`/`RIVERSIDE`/`GENTLE_SLOPE`)와 1:1로 안 맞아서 일단 안 보낸다. */
    private suspend fun recommendRoutes() {
        try {
            val mapVersion = MetaHolder.state.value?.mapVersions?.current
                ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current
            val distanceM = parseDistanceMeters(currentState.distanceLabel)
            val response = routesApi.fetchRoutes(
                RoutesRequest(
                    mapVersion = mapVersion,
                    start = TEMP_START_LOCATION,
                    distanceM = distanceM,
                    preferUnexplored = currentState.avoidVisitedPreferred,
                ),
            )
            Log.i(TAG, "추천 성공 routes=${response.routes.size} warnings=${response.warnings}")
            CourseRecommendResultHolder.publish(CourseRecommendResult.Success(response))
        } catch (e: Exception) {
            Log.w(TAG, "추천 실패", e)
            CourseRecommendResultHolder.publish(
                CourseRecommendResult.Failure(e.message ?: "코스를 추천받지 못했어요"),
            )
        }
        sendEffect(CourseCreateEffect.NavigateToResult)
    }

    /** "5km"/"12.5km" → 5000/12500. 프리셋·커스텀 값 전부 0.5km 단위라 서버의 500m 단위 제약과
     * 맞는다. */
    private fun parseDistanceMeters(distanceLabel: String): Int =
        (distanceLabel.removeSuffix("km").toDouble() * 1000).toInt()
}
