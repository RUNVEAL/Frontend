package com.sloth.runveal.tabrecord.contract

import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiEffect
import com.sloth.runveal.ui.mvi.UiIntent
import com.sloth.runveal.ui.mvi.UiState

sealed interface RecordDetailIntent : UiIntent {
    /** 화면에 들어오면서 어떤 기록인지 알려준다 — 라우트는 [trackId]만 들고 있고 값은 DB에서 읽는다. */
    data class Load(val trackId: String) : RecordDetailIntent

    /** 더보기 메뉴에서 "이름 수정"을 골랐다 — 지금 이름을 채운 입력 시트를 띄운다. */
    data object RenameClicked : RecordDetailIntent

    /** 입력 시트에서 "저장"을 눌렀다. 앞뒤 공백은 ViewModel이 다듬고, 빈 이름은 무시한다. */
    data class RenameConfirmed(val name: String) : RecordDetailIntent

    data object RenameDismissed : RecordDetailIntent

    /** 더보기 메뉴에서 "삭제"를 골랐다 — 되돌릴 수 없는 일이라 바로 지우지 않고 한 번 더 묻는다. */
    data object DeleteClicked : RecordDetailIntent

    data object DeleteConfirmed : RecordDetailIntent

    data object DeleteDismissed : RecordDetailIntent
}

sealed interface RecordDetailEffect : UiEffect {
    /** 기록을 지웠다 — 보여줄 게 없으니 목록으로 돌아간다. */
    data object NavigateBack : RecordDetailEffect
}

/**
 * "구간 기록" 한 줄 — 1km마다 끊은 페이스. [barRatio]는 막대 길이(0~1)로, **빠른 구간일수록 길다**
 * (가장 빠른 구간이 꽉 찬다).
 */
data class RecordDetailSplit(val label: String, val paceLabel: String, val barRatio: Float)

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
        val splits: List<RecordDetailSplit>,
        /** 매칭 경로가 없으면(매칭 실패·점 부족) null이라 지도 자리에 플레이스홀더만 보인다. */
        val mapSnapshotRequest: SnapshotRequest?,
        /** 삭제 확인 다이얼로그가 떠 있는지. */
        val isDeleteDialogVisible: Boolean = false,
        /** 이름 수정 시트가 떠 있는지. */
        val isRenameSheetVisible: Boolean = false,
    ) : RecordDetailState
}
