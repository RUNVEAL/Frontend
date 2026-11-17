package com.sloth.runveal.courserecommend

import com.sloth.runveal.network.routes.RoutesResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 결과 화면 상단에 그대로 보여줄 B0 설정값 — 거리, 안 가본 길 우선, 적용한 필터 수. */
data class CourseRecommendSettings(
    val distanceLabel: String,
    val avoidVisitedPreferred: Boolean,
    val selectedFilterCount: Int,
)

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
