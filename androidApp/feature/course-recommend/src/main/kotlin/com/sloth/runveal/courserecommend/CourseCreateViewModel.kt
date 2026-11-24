package com.sloth.runveal.courserecommend

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.sloth.runveal.courserecommend.contract.CourseCreateEffect
import com.sloth.runveal.courserecommend.contract.CourseCreateIntent
import com.sloth.runveal.courserecommend.contract.CourseCreateState
import com.sloth.runveal.courserecommend.contract.DefaultCustomDistanceIndex
import com.sloth.runveal.courserecommend.contract.DefaultDistance
import com.sloth.runveal.courserecommend.contract.LocationSelectionTarget
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CourseRecommendDebug"

/** TODO: 임시 로딩 — 디자인 적용된 로딩으로 교체되면 이 최소 노출 시간도 다시 검토.
 * 응답이 이것보다 빨리 와도 로딩 모션이 최소 한 번은 눈에 보이게 그만큼은 붙잡아 둔다 — 안 그러면
 * 응답이 너무 빨라 Lottie 컴포지션이 로드되기도 전에 화면이 넘어가 버려서 로딩이 거의 안 보인다. */
private const val MIN_RECOMMEND_LOADING_MS = 900L

class CourseCreateViewModel : MviViewModel<CourseCreateIntent, CourseCreateState, CourseCreateEffect>(
    initialState = CourseCreateState(),
) {
    private val recommender = CourseRecommender()
    private val dongApi = DongApi()

    /** 진행 중인 행정동 조회 — 지도를 연달아 탭하면 앞선 요청을 버린다. */
    private var placeNameJob: Job? = null

    override suspend fun handleIntent(intent: CourseCreateIntent) {
        when (intent) {
            CourseCreateIntent.StartLocationClicked ->
                setState { copy(locationSelectionTarget = LocationSelectionTarget.Start) }

            CourseCreateIntent.EndLocationClicked ->
                setState { copy(locationSelectionTarget = LocationSelectionTarget.End) }

            CourseCreateIntent.CancelLocationSelection -> {
                placeNameJob?.cancel()
                setState {
                    copy(
                        locationSelectionTarget = LocationSelectionTarget.None,
                        pickedLocation = null,
                        pickedPlaceName = null,
                    )
                }
            }

            CourseCreateIntent.ConfirmLocationSelection -> setState {
                when (locationSelectionTarget) {
                    LocationSelectionTarget.Start -> copy(
                        startLocation = selectedPlaceName,
                        startLatLon = pickedLocation ?: startLatLon,
                        locationSelectionTarget = LocationSelectionTarget.None,
                        pickedLocation = null,
                        pickedPlaceName = null,
                    )
                    LocationSelectionTarget.End -> copy(
                        endLocation = selectedPlaceName,
                        endLocationIsDefault = false,
                        endLatLon = pickedLocation ?: endLatLon,
                        locationSelectionTarget = LocationSelectionTarget.None,
                        pickedLocation = null,
                        pickedPlaceName = null,
                    )
                    LocationSelectionTarget.None -> this
                }
            }

            is CourseCreateIntent.MapLocationPicked -> {
                val location = intent.location
                // 옛 이름을 지우고 넣는다 — 안 지우면 새로 찍은 자리에 먼 동 이름이 잠깐 남는다.
                setState { copy(pickedLocation = location, pickedPlaceName = null) }
                lookUpPlaceName(location)
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

            is CourseCreateIntent.RecommendClicked -> recommendRoutes(intent.currentLocation)
        }
    }

    /**
     * 탭한 자리의 행정동 이름을 받아 온다(`GET /api/v1/dong`). 못 받으면 상태를 건드리지 않아서
     * 화면은 좌표를 그대로 보여준다 — 위치를 고르는 일 자체는 이름 없이도 되므로 오류로 다루지 않는다.
     *
     * 지도를 연달아 탭하면 앞선 요청을 취소하고, 그래도 늦게 도착한 응답은 좌표가 그대로인지
     * 확인한 뒤에만 반영한다 — 취소가 응답 처리 직전에 걸리면 엉뚱한 자리의 이름이 박힌다.
     */
    private fun lookUpPlaceName(location: LatLon) {
        placeNameJob?.cancel()
        placeNameJob = viewModelScope.launch {
            when (val result = dongApi.fetchDong(lat = location.lat, lon = location.lon)) {
                is DongResult.Success -> setState {
                    if (pickedLocation == location) copy(pickedPlaceName = result.dong.shortName) else this
                }

                DongResult.OutOfServiceArea -> Log.i(TAG, "서비스 지역 밖 — 좌표를 그대로 보여준다")

                is DongResult.Failure -> Log.w(TAG, "동 조회 실패 — ${result.debugMessage}")
            }
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
        val startedAtMs = System.currentTimeMillis()
        val settings = currentState.toSettings(currentLocation)
        CourseRecommendResultHolder.publish(recommender.recommend(settings, currentLocation))
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        if (elapsedMs < MIN_RECOMMEND_LOADING_MS) delay(MIN_RECOMMEND_LOADING_MS - elapsedMs)
        sendEffect(CourseCreateEffect.NavigateToResult)
    }

    /**
     * 지금 패널에 고른 조건을 그대로 추천 입력으로 옮긴다 — 결과 화면에서 조건을 다시 바꿀 때도 같은
     * 값을 이어서 쓴다([CourseRecommendSettings]).
     *
     * 출발지를 안 골랐으면 이번에 실제로 쓴 현재 위치를 박아 둔다 — 안 그러면 결과 화면에서 조건만
     * 바꿔 다시 받을 때 출발지가 비어 임시 좌표로 떨어진다.
     */
    private fun CourseCreateState.toSettings(currentLocation: LatLon?): CourseRecommendSettings = CourseRecommendSettings(
        selectedDistance = selectedDistance,
        isCustomDistanceSelected = isCustomDistanceSelected,
        customDistanceIndex = customDistanceIndex,
        avoidVisitedPreferred = avoidVisitedPreferred,
        selectedFilters = selectedFilters,
        startLatLon = startLatLon ?: currentLocation,
        endLatLon = endLatLon,
    )

}
