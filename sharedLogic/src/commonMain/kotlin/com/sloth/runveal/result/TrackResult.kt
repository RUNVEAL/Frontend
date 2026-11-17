package com.sloth.runveal.result

import com.sloth.runveal.map.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 러닝 결과 화면의 "동네 탐험률" 한 줄 — 이번 트랙 기록 전/후 완주율(%). */
data class DongRateChange(
    val admCd2: String,
    val name: String,
    val beforePercent: Int,
    val afterPercent: Int,
)

/** 러닝 결과 화면이 그대로 쓰는 최종 데이터 — 최종 매칭(§6)이 끝나야 만들어진다.
 * [routePaths]는 결과 카드 지도 스냅샷(`docs/running/COURSE_MAP_SNAPSHOT.md`) 생성용 — 도로 매칭에
 * 성공하면 **실제로 달린 자리만 남긴 매칭 경로**(지도의 가본 길과 같은 선)이고, 실패하면 필터링된
 * GPS 경로 한 줄이다. 조각 목록인 이유는 [com.sloth.runveal.match.MatchedPathFilter]가 GPS와
 * 동떨어진 부분을 잘라내면서 선이 나뉘기 때문이다. */
data class TrackResult(
    val trackId: String,
    val startedAtEpochMs: Long,
    val distanceMeters: Double,
    val elapsedMs: Long,
    val newRoadMeters: Double,
    val dongStats: List<DongRateChange>,
    val routePaths: List<List<LatLon>>,
    /** 이번에 처음 간 구간들 — 결과 카드 지도에서 [routePaths] 위에 금색으로 덧그린다. 매칭이
     * 실패하면 비어 있고, 그때는 전부 한 색("오늘 달린 길")으로만 그려진다. */
    val newRoadPaths: List<List<LatLon>> = emptyList(),
)

/**
 * 기록 종료 → 최종 매칭 완료 사이의 간극을 메우는 StateFlow 싱글톤. `TrackingService`(androidMain)가
 * 매칭이 끝나는 대로 [publish]하고, 결과 화면이 이 값이 채워질 때까지 로딩 상태로 기다린다.
 */
object TrackResultHolder {
    private val _state = MutableStateFlow<TrackResult?>(null)
    val state: StateFlow<TrackResult?> = _state.asStateFlow()

    fun publish(result: TrackResult) {
        _state.value = result
    }

    fun clear() {
        _state.value = null
    }
}
