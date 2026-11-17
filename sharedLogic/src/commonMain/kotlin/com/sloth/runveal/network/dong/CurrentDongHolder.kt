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
 * 보는 쪽이 각자 대체 문구를 쓴다.
 */
object CurrentDongHolder {
    private val _state = MutableStateFlow<DongResponse?>(null)
    val state: StateFlow<DongResponse?> = _state.asStateFlow()

    fun publish(dong: DongResponse) {
        _state.value = dong
    }

    fun clear() {
        _state.value = null
    }
}
