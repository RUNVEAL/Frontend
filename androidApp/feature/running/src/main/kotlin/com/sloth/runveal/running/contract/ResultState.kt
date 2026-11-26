package com.sloth.runveal.running.contract

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiState

data class ResultDongStat(val name: String, val beforePercent: Int, val afterPercent: Int)

/** [TrackResultHolder][com.sloth.runveal.result.TrackResultHolder]가 비어 있는 동안은 [Loading] —
 * 최종 매칭(§6)이 끝나야 [Ready]가 된다(§9 "종료 직후 최종본이 오기 전까지" 관련 결정). */
sealed interface ResultState : UiState {
    data object Loading : ResultState

    data class Ready(
        val dateLabel: String,
        val newRoadLabel: String,
        val dongStats: List<ResultDongStat>,
        val distanceLabel: String,
        val elapsedLabel: String,
        val paceLabel: String,
        /** 경로 점이 2개 미만(제자리 종료 등)이면 `null` — 지도 카드가 그때는 플레이스홀더만 보여준다. */
        val mapSnapshotRequest: SnapshotRequest?,
        /**
         * 이미지 저장 템플릿이 경로를 **벡터로 직접** 그릴 때 쓰는 점들
         * (`com.sloth.runveal.running.component.RouteVectorMap`).
         *
         * [mapSnapshotRequest]와 같은 경로지만 GeoJSON이 아니라 좌표 그대로다 — 저장 이미지는 지도
         * 타일을 깔지 않아서(네트워크·저작권 표기) 점을 직접 받아 선으로 그린다.
         */
        val routePaths: List<List<LatLon>>,
        /** [routePaths] 위에 금색으로 덧그릴 "이번에 처음 간" 구간. 매칭이 실패하면 비어 있다. */
        val newRoadPaths: List<List<LatLon>>,
    ) : ResultState
}
