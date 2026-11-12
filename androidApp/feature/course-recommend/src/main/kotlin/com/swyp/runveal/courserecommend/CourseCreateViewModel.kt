package com.swyp.runveal.courserecommend

import android.util.Log
import com.swyp.runveal.courserecommend.contract.CourseCreateEffect
import com.swyp.runveal.courserecommend.contract.CourseCreateIntent
import com.swyp.runveal.courserecommend.contract.CourseCreateState
import com.swyp.runveal.courserecommend.contract.DefaultCustomDistanceIndex
import com.swyp.runveal.courserecommend.contract.DefaultDistance
import com.swyp.runveal.courserecommend.contract.LocationSelectionTarget
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.network.routes.LatLng
import com.swyp.runveal.network.routes.RoutesApi
import com.swyp.runveal.network.routes.RoutesRequest
import com.swyp.runveal.ui.mvi.MviViewModel

private const val TAG = "CourseRecommendDebug"

/** TEMP — 출발지를 아직 안 골랐고 현재 위치도 못 받았을 때 쓰는 최후 기본 좌표(성수동 인근). */
private val TEMP_START_LOCATION = LatLon(lat = 37.5445, lon = 127.0557)

private fun LatLon.toRoutesLatLng(): LatLng = LatLng(lat = lat, lng = lon)

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
                setState { copy(locationSelectionTarget = LocationSelectionTarget.None, pickedLocation = null) }

            CourseCreateIntent.ConfirmLocationSelection -> setState {
                when (locationSelectionTarget) {
                    LocationSelectionTarget.Start -> copy(
                        startLocation = selectedPlaceName,
                        startLatLon = pickedLocation ?: startLatLon,
                        locationSelectionTarget = LocationSelectionTarget.None,
                        pickedLocation = null,
                    )
                    LocationSelectionTarget.End -> copy(
                        endLocation = selectedPlaceName,
                        endLocationIsDefault = false,
                        endLatLon = pickedLocation ?: endLatLon,
                        locationSelectionTarget = LocationSelectionTarget.None,
                        pickedLocation = null,
                    )
                    LocationSelectionTarget.None -> this
                }
            }

            is CourseCreateIntent.MapLocationPicked ->
                setState { copy(pickedLocation = intent.location) }

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

            is CourseCreateIntent.RecommendClicked -> recommendRoutes(intent.currentLocation)
        }
    }

    /** `docs/api/API.md` §3.3 — B0 설정값으로 `/api/v1/routes`를 호출하고, 성공·실패 모두
     * [CourseRecommendResultHolder]에 publish한 뒤 결과 화면으로 넘어간다. `selectedFilters`는 화면
     * 라벨(한글)이라 서버 필터 코드(`PARK`/`RIVERSIDE`/`GENTLE_SLOPE`)와 1:1로 안 맞아서 일단 안 보낸다.
     * `end`는 지도에서 도착지를 직접 고른 경우에만 보낸다 — 안 고르면(기본값 "출발지로 돌아오기")
     * `endLatLon`이 null이라 자동으로 순환(LOOP) 코스가 된다(§3.3.1).
     *
     * 출발지를 한 번도 안 골랐으면([startLatLon] null) [currentLocation](화면이 미리 받아 둔 현재
     * 위치)을 쓰고, 그마저도 못 받았으면(권한 없음·위치 서비스 꺼짐 등) [TEMP_START_LOCATION]으로
     * 떨어진다. */
    private suspend fun recommendRoutes(currentLocation: LatLon?) {
        val settings = CourseRecommendSettings(
            distanceLabel = currentState.distanceLabel,
            avoidVisitedPreferred = currentState.avoidVisitedPreferred,
            selectedFilterCount = currentState.selectedFilters.size,
        )
        try {
            val mapVersion = MetaHolder.state.value?.mapVersions?.current
                ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current
            val distanceM = parseDistanceMeters(currentState.distanceLabel)
            val response = routesApi.fetchRoutes(
                RoutesRequest(
                    mapVersion = mapVersion,
                    start = (currentState.startLatLon ?: currentLocation ?: TEMP_START_LOCATION).toRoutesLatLng(),
                    end = currentState.endLatLon?.toRoutesLatLng(),
                    distanceM = distanceM,
                    preferUnexplored = currentState.avoidVisitedPreferred,
                ),
            )
            Log.i(TAG, "추천 성공 routes=${response.routes.size} warnings=${response.warnings}")
            CourseRecommendResultHolder.publish(CourseRecommendResult.Success(response, settings))
        } catch (e: Exception) {
            Log.w(TAG, "추천 실패", e)
            CourseRecommendResultHolder.publish(
                CourseRecommendResult.Failure(e.message ?: "코스를 추천받지 못했어요", settings),
            )
        }
        sendEffect(CourseCreateEffect.NavigateToResult)
    }

    /** "5km"/"12.5km" → 5000/12500. 프리셋·커스텀 값 전부 0.5km 단위라 서버의 500m 단위 제약과
     * 맞는다. */
    private fun parseDistanceMeters(distanceLabel: String): Int =
        (distanceLabel.removeSuffix("km").toDouble() * 1000).toInt()
}
