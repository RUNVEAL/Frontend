package com.sloth.runveal.courserecommend

import com.sloth.runveal.courserecommend.contract.CustomDistanceValues
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.routes.RoutesResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 이 추천을 만든 조건. 결과 화면 상단 칩으로 보여주는 값이면서, **그 화면에서 조건을 바꿔 다시
 * 추천받을 때 쓰는 입력**이기도 하다(출발·도착 좌표까지 들고 있는 이유).
 */
data class CourseRecommendSettings(
    val selectedDistance: String,
    val isCustomDistanceSelected: Boolean,
    val customDistanceIndex: Int,
    val avoidVisitedPreferred: Boolean,
    val selectedFilters: Set<String>,
    /** 코스 만들기에서 고른 출발·도착지. 안 골랐으면 null — 현재 위치로 떨어진다. */
    val startLatLon: LatLon? = null,
    val endLatLon: LatLon? = null,
) {
    /** 칩과 요청에 쓰는 "5km"/"12.5km". */
    val distanceLabel: String
        get() = if (isCustomDistanceSelected) "${CustomDistanceValues[customDistanceIndex]}km" else selectedDistance

    val selectedFilterCount: Int get() = selectedFilters.size
}

/** `CourseCreateViewModel`이 `/api/v1/routes` 응답을 받으면 publish한다 — 복잡한 응답 객체를 네비게이션
 * 인자로 못 넘기니 홀더로 전달한다(`TrackResultHolder`와 같은 패턴). 결과 화면(`CourseRecommendResultScreen`)이
 * 구독해서 그대로 그린다. */
sealed interface CourseRecommendResult {
    val settings: CourseRecommendSettings

    data class Success(val response: RoutesResponse, override val settings: CourseRecommendSettings) : CourseRecommendResult
    data class Failure(val message: String, override val settings: CourseRecommendSettings) : CourseRecommendResult
}

object CourseRecommendResultHolder {
    private val _state = MutableStateFlow<CourseRecommendResult?>(null)
    val state: StateFlow<CourseRecommendResult?> = _state.asStateFlow()

    fun publish(result: CourseRecommendResult) {
        _state.value = result
    }

    fun clear() {
        _state.value = null
    }
}
