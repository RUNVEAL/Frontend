package com.swyp.runveal.tabmypage

import androidx.lifecycle.viewModelScope
import com.swyp.runveal.external.ImportResult
import com.swyp.runveal.external.ImportResultHolder
import com.swyp.runveal.map.snapshot.SnapshotVariant
import com.swyp.runveal.map.snapshot.toMultiPathSnapshotRequest
import com.swyp.runveal.tabmypage.contract.ImportDongStat
import com.swyp.runveal.tabmypage.contract.ImportRecordsResultState
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.launch

/**
 * 가져오기가 올려 둔 결과([ImportResultHolder])를 화면이 쓸 모양으로 바꿔 준다 — 러닝 결과 화면
 * (`feature:running`의 `ResultViewModel`)과 같은 구조다. 사용자가 보낼 의도가 없는 화면(버튼은
 * 전부 네비게이션)이라 Intent·Effect 타입 인자는 [Nothing]이다.
 */
class ImportRecordsResultViewModel : MviViewModel<Nothing, ImportRecordsResultState, Nothing>(
    initialState = ImportRecordsResultState.Loading,
) {
    init {
        viewModelScope.launch {
            ImportResultHolder.state.collect { result ->
                setState { result?.let(::toState) ?: ImportRecordsResultState.Loading }
            }
        }
    }

    override suspend fun handleIntent(intent: Nothing) = Unit

    /** 한 건도 못 가져왔으면 보여 줄 집계가 없다 — 빈 상태 화면(`A4-4`)으로 간다. */
    private fun toState(result: ImportResult): ImportRecordsResultState =
        if (result.imported == 0) ImportRecordsResultState.Empty else result.toReadyState()

    private fun ImportResult.toReadyState() = ImportRecordsResultState.Ready(
        importedCount = imported,
        newRoadLabel = "%.1f".format(newRoadMeters / 1000.0),
        dongStats = dongStats.map { ImportDongStat(it.name, it.beforePercent, it.afterPercent) },
        // 매칭이 실패한 기록(failed)은 여기 넣지 않는다 — "경로가 없는 기록"이라는 말이 맞지 않다.
        // TODO: 매칭 실패분을 어떻게 알릴지 시안이 정해지면 그 줄을 따로 붙인다.
        noRouteCount = skipped,
        // 여러 기록을 한 장에 담는 지도 — 기록마다 선 하나씩, 그중 처음 간 구간만 금색이다.
        // ownerId는 스냅샷 캐시 키라서 가져온 건수·거리로 묶어 둔다(같은 결과면 같은 그림).
        mapSnapshotRequest = routePaths.toMultiPathSnapshotRequest(
            ownerId = "import_${imported}_${newRoadMeters.toLong()}",
            variant = SnapshotVariant.RESULT_CARD,
            newRoadPaths = newRoadPaths,
            // 기록이 여러 건이라 어느 것의 시작인지 가릴 수 없다 — 시작·끝 마커를 두지 않는다.
            withEndpoints = false,
        ),
    )
}
