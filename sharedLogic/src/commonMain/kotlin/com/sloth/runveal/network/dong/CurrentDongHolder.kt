package com.sloth.runveal.network.dong

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 지금 서 있는 행정동. 조회는 홈 지도 오버레이(`HomeMapOverlayViewModel`)가 한 번만 하고, 그 값을
 * 여기 올려 **다른 화면도 같이 본다** — 줌아웃 요약 카드는 지도 오버레이와 다른 모듈(`feature:home`)
 * 에 있어서 상태를 직접 받을 수 없다.
 *
 * 홀더를 쓰는 다른 전역 상태([com.sloth.runveal.network.meta.MetaHolder],
 * [com.sloth.runveal.result.TrackResultHolder])와 같은 이유·같은 모양이다.
 *
 * 아직 못 받았으면 null — 위치 권한이 없거나 서비스 지역 밖인 건 흔한 상황이라 오류로 다루지 않고,
 * 보는 쪽이 각자 대체 문구를 쓴다. 그래서 null만으로는 **서버에서 못 받은 것**과 구분이 안 된다 —
 * 그건 [loadFailed]가 따로 알린다.
 */
object CurrentDongHolder {
    private val _state = MutableStateFlow<DongResponse?>(null)
    val state: StateFlow<DongResponse?> = _state.asStateFlow()

    /**
     * 서버에서 동을 **못 받았는지**(오프라인·서버 꺼짐·타임아웃). 위치 권한 없음이나 서비스 지역
     * 밖은 여기 들어오지 않는다 — 정상적인 상황이라 안내할 게 없다. 홈이 이 값으로 "데이터를 받지
     * 못했어요" 배너를 띄운다.
     */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    /**
     * "다시 시도"를 누른 횟수. 동 조회는 홈 지도 오버레이(`feature:tab-home`)가 하고 배너는
     * `feature:home`에 있어서 서로를 직접 부를 수 없다 — 누른 걸 여기 올려 두면 조회하는 쪽이
     * 구독해 다시 받아 온다(값 자체는 쓰지 않고 바뀌었다는 사실만 쓴다).
     */
    private val _retryRequests = MutableStateFlow(0)
    val retryRequests: StateFlow<Int> = _retryRequests.asStateFlow()

    fun publish(dong: DongResponse) {
        _state.value = dong
        _loadFailed.value = false
    }

    /** 서버에서 못 받았다 — 값은 그대로 두고(이전에 받아 둔 게 있으면 계속 보여 준다) 실패만 표시한다. */
    fun publishFailure() {
        _loadFailed.value = true
    }

    /** 서비스 지역 밖처럼 **정상적으로** 동이 없는 경우 — 실패 표시를 지운다. */
    fun publishUnavailable() {
        _loadFailed.value = false
    }

    fun requestRetry() {
        _retryRequests.value++
    }

    fun clear() {
        _state.value = null
        _loadFailed.value = false
    }
}
