package com.sloth.runveal.tabmypage.contract

import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiState

/** 탐험률이 오른 동 한 줄 — 이번 가져오기 전/후(%). `feature:running`의 `ResultDongStat`과 같은 모양. */
data class ImportDongStat(val name: String, val beforePercent: Int, val afterPercent: Int)

/**
 * 기록 불러오기 결과 화면 상태. 가져오기가 결과를 올려 줄 때까지는 [Loading]이다 — 보통은 진행
 * 화면에서 넘어오는 순간 이미 채워져 있지만, 프로세스가 죽었다 복구된 경우엔 비어 있다.
 */
sealed interface ImportRecordsResultState : UiState {
    data object Loading : ImportRecordsResultState

    /**
     * 가져온 기록이 하나도 없다(디자인 `A4-4`). Health Connect에 달리기 세션이 없거나, 있어도
     * 경로(GPS)가 없어 전부 건너뛴 경우다 — 둘 다 "경로가 있는 기록이 없다"로 귀결돼서 한 화면으로
     * 묶는다. 보여 줄 숫자가 없어 payload도 없다.
     */
    data object Empty : ImportRecordsResultState

    /**
     * @param newRoadLabel 새로 탐험한 길(km, 소수 한 자리) — 단위는 화면이 붙인다.
     * @param noRouteCount 경로가 없어 지도에 올리지 못한 기록 수. 0이면 화면에서 그 줄을 뺀다.
     * @param mapSnapshotRequest 가져온 경로를 모두 그린 지도 — 그릴 선이 없으면 null이다.
     */
    data class Ready(
        val importedCount: Int,
        val newRoadLabel: String,
        val dongStats: List<ImportDongStat>,
        val noRouteCount: Int,
        val mapSnapshotRequest: SnapshotRequest?,
    ) : ImportRecordsResultState
}
