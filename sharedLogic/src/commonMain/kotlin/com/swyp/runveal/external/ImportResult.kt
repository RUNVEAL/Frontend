package com.swyp.runveal.external

import com.swyp.runveal.map.LatLon
import com.swyp.runveal.result.DongRateChange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기록 불러오기 결과 화면이 그대로 쓰는 최종 데이터. 러닝 한 건을 끝낸 결과
 * ([com.swyp.runveal.result.TrackResult])와 성질이 같고, 다른 점은 **여러 기록을 합친 값**이라는
 * 것이다 — 그래서 시작·끝 시각이나 페이스처럼 한 건에만 뜻이 있는 값은 담지 않는다.
 *
 * @param imported 가져와 매칭까지 끝낸 기록 수.
 * @param skipped 경로(GPS)가 없어 도로에 매칭할 수 없던 기록 수. 실패가 아니라 "가져올 수 없는 기록"이다.
 * @param failed 매칭이 끝까지 실패한 기록 수. 거리·시간은 남아 있어 나중에 다시 매칭할 대상이다.
 * @param newRoadMeters 이번에 처음 간 길의 총 길이.
 * @param routePaths 가져온 기록들의 매칭 경로(기록마다 한 줄) — 결과 지도에 모두 그린다.
 * @param newRoadPaths 그중 처음 간 구간들 — [routePaths] 위에 금색으로 덧그린다.
 */
data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
    val newRoadMeters: Double,
    val dongStats: List<DongRateChange>,
    val routePaths: List<List<LatLon>>,
    val newRoadPaths: List<List<LatLon>>,
)

/**
 * 가져오기 완료 → 결과 화면 사이의 간극을 메우는 StateFlow 싱글톤
 * ([com.swyp.runveal.result.TrackResultHolder]와 같은 역할). 진행 화면은 라우트에 싣기 어려운
 * 경로·동 목록까지 넘겨야 해서 이 홀더를 거친다.
 */
object ImportResultHolder {
    private val _state = MutableStateFlow<ImportResult?>(null)
    val state: StateFlow<ImportResult?> = _state.asStateFlow()

    fun publish(result: ImportResult) {
        _state.value = result
    }

    fun clear() {
        _state.value = null
    }
}
