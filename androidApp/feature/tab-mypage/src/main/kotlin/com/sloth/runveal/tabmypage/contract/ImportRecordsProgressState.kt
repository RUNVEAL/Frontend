package com.sloth.runveal.tabmypage.contract

import com.sloth.runveal.external.ImportProgress
import com.sloth.runveal.ui.mvi.UiState

/**
 * 기록 불러오기 진행 화면 상태. [progress]는 가져오기 로직이
 * [com.sloth.runveal.external.ImportProgressHolder]로 내보내는 값을 그대로 옮긴 것이다.
 *
 * 화면은 아직 어떤 단계든 같은 로딩 모션만 보여 주지만, 단계별 문구·진행률 시안이 나오면 이 값을
 * 그대로 쓰면 된다.
 */
data class ImportRecordsProgressState(
    val progress: ImportProgress = ImportProgress.Scanning,
) : UiState
