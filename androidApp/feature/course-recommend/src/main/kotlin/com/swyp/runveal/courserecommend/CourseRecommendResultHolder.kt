package com.swyp.runveal.courserecommend

import com.swyp.runveal.network.routes.RoutesResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** `CourseCreateViewModel`이 `/api/v1/routes` 응답을 받으면 publish한다 — 복잡한 응답 객체를 네비게이션
 * 인자로 못 넘기니 홀더로 전달한다(`TrackResultHolder`와 같은 패턴). 결과 화면(`CourseRecommendResultScreen`)이
 * 구독해서 그대로 그린다. */
sealed interface CourseRecommendResult {
    data class Success(val response: RoutesResponse) : CourseRecommendResult
    data class Failure(val message: String) : CourseRecommendResult
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
