package com.sloth.runveal.tabrecord.contract

import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiIntent
import com.sloth.runveal.ui.mvi.UiState

sealed interface RecordDetailIntent : UiIntent {
    /** 화면에 들어오면서 어떤 기록인지 알려준다 — 라우트는 [trackId]만 들고 있고 값은 DB에서 읽는다. */
    data class Load(val trackId: String) : RecordDetailIntent
}

/** "탐험률 변화" 한 줄 — 이 러닝 직전과 직후의 동네 탐험률(%). */
data class RecordDetailDongStat(val name: String, val beforePercent: Int, val afterPercent: Int)

sealed interface RecordDetailState : UiState {
    data object Loading : RecordDetailState

    /** 목록에서 지운 기록을 백스택으로 되돌아와 다시 열었을 때처럼, 읽을 기록이 없는 경우. */
    data object NotFound : RecordDetailState

    /** 전부 화면에 그대로 찍히는 라벨이다(포맷은 [RecordDetailViewModel][com.sloth.runveal.tabrecord.RecordDetailViewModel]). */
    data class Ready(
        val title: String,
        val dateTimeLabel: String,
        val distanceValue: String,
        val newRoadValue: String,
        val elapsedLabel: String,
        val paceLabel: String,
        /** 칼로리는 아직 저장하는 값이 없어 "-"로 둔다 — 자리는 시안대로 지킨다. */
        val caloriesLabel: String,
        val dongStats: List<RecordDetailDongStat>,
        /** 매칭 경로가 없으면(매칭 실패·점 부족) null이라 지도 자리에 플레이스홀더만 보인다. */
        val mapSnapshotRequest: SnapshotRequest?,
    ) : RecordDetailState
}
