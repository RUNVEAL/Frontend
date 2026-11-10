package com.swyp.runveal.tabrecord.contract

import com.swyp.runveal.map.snapshot.SnapshotRequest
import com.swyp.runveal.ui.mvi.UiState

data class ResultDongStat(val name: String, val beforePercent: Int, val afterPercent: Int)

/** [TrackResultHolder][com.swyp.runveal.result.TrackResultHolder]가 비어 있는 동안은 [Loading] —
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
    ) : ResultState
}
