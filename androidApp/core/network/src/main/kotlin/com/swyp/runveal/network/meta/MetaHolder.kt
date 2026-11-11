package com.swyp.runveal.network.meta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 앱 실행 시 받아 온 `/api/v1/meta` 결과를 들고 있는 싱글톤 — `SplashGraph`가 채우고, 코스 추천 화면
 * (슬라이더 범위·필터 칩, `docs/api/API.md` §3.1.1) 등 나중에 값이 필요한 곳이 구독해서 읽는다.
 */
object MetaHolder {
    private val _state = MutableStateFlow<MetaResponse?>(null)
    val state: StateFlow<MetaResponse?> = _state.asStateFlow()

    fun publish(meta: MetaResponse) {
        _state.value = meta
    }
}
