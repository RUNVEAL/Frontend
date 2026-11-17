package com.sloth.runveal.result

import com.sloth.runveal.map.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 러닝 중 주기적으로 받는 도로 매칭 결과(도로에 맞춰진 경로선) — `TrackingService`(androidMain)가
 * 녹화 중 일정 간격으로 `POST /api/v1/match`(`purpose: LIVE`)를 호출해 성공할 때마다 [publish]한다.
 * 지도 쪽(`HomeScreen`)이 이 값이 있으면 원본 GPS 점 대신 이 경로로 "지금 달리는 선"을 그린다.
 *
 * 경로가 **조각 목록**인 이유: 매칭 경로에서 실제로 달린 자리와 동떨어진 부분을 잘라내면
 * ([com.sloth.runveal.match.MatchedPathFilter]) 선이 여러 조각으로 나뉜다. 다시 이어 붙이면 잘라낸
 * 구간을 직선으로 바꿔 그릴 뿐이라 조각째로 들고 다닌다.
 *
 * 다음 매칭이 올 때까지, 또는 실패했을 때는 마지막으로 성공한 선을 그대로 유지한다 — 매칭 간격보다
 * 짧은 주기로 다시 그리는 화면 쪽이 그 사이엔 원본 점으로 자연스럽게 이어 그린다. 새 러닝을 시작하면
 * [clear]로 지난 러닝의 선이 남지 않게 한다.
 */
object LiveMatchHolder {
    private val _state = MutableStateFlow<List<List<LatLon>>?>(null)
    val state: StateFlow<List<List<LatLon>>?> = _state.asStateFlow()

    fun publish(paths: List<List<LatLon>>) {
        _state.value = paths
    }

    fun clear() {
        _state.value = null
    }
}
