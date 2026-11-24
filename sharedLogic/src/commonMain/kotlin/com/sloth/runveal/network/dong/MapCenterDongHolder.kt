package com.sloth.runveal.network.dong

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 지금 **지도 가운데에 보이는** 행정동. 홈 줌아웃 화면의 상단 라벨과 하단 요약 카드가 같이 본다 —
 * 조회는 홈 지도 오버레이(`HomeMapOverlayViewModel`)가 하고, 하단 패널은 다른 모듈(`feature:home`)
 * 이라 상태를 직접 받을 수 없어서 [CurrentDongHolder]와 같은 패턴으로 여기 올린다.
 *
 * **[CurrentDongHolder]와 다른 값이다.** 그쪽은 "지금 내가 서 있는 동"이고 탐험률 집계·러닝 기록이
 * 기준으로 쓴다. 이건 지도를 움직이면 따라 바뀌는 "보고 있는 동"이라, 둘을 한 홀더에 합치면 지도를
 * 끌었을 때 러닝 쪽 기준까지 흔들린다.
 *
 * 둘을 비교해 같을 때만 요약 카드에 "내 동네" 꼬리표가 붙는다 — 남의 동네를 보고 있는데 내 동네라고
 * 하면 틀린 말이 된다.
 */
object MapCenterDongHolder {
    private val _state = MutableStateFlow<DongResponse?>(null)
    val state: StateFlow<DongResponse?> = _state.asStateFlow()

    fun publish(dong: DongResponse) {
        _state.value = dong
    }

    /** 서비스 지역 밖(바다·해외)이나 조회 실패 — 이름을 비워 호출부가 대체 문구를 쓰게 한다. */
    fun clear() {
        _state.value = null
    }
}
