package com.sloth.runveal.tabmypage

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sloth.runveal.external.ExternalWorkoutPoint
import com.sloth.runveal.external.ImportProgress
import com.sloth.runveal.external.ImportProgressHolder
import com.sloth.runveal.health.HealthConnectImporter
import com.sloth.runveal.tabmypage.contract.ImportRecordsProgressEffect
import com.sloth.runveal.tabmypage.contract.ImportRecordsProgressState
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 화면에 들어온 순간 가져오기를 시작하고, 끝나면 [ImportRecordsProgressEffect.Finished]를 보낸다.
 * 사용자가 누를 게 없는 화면이라 Intent 타입은 [Nothing]이다.
 *
 * 가져오기가 `viewModelScope`에 매여 있어서 **화면을 벗어나면 중단된다.** 한 건을 끝낼 때마다 재개
 * 지점이 전진하므로(`ImportCheckpointStore`) 중간에 끊겨도 다음 실행이 이어 가고, 중복은
 * `external_id`가 막는다 — 다만 앱을 벗어나도 계속 받으려면 WorkManager로 옮겨야 한다.
 *
 * **백그라운드로 옮기면 안 된다.** 다른 앱이 기록한 경로는 화면이 떠 있을 때만 읽을 수 있다 —
 * 백그라운드에서는 "항상 허용" 상태여도 `ConsentRequired`가 돌아온다
 * (`rememberExerciseRouteConsentRequester` 주석). 수십 건이면 몇 분 걸리는 건 감수한다.
 */
class ImportRecordsProgressViewModel(
    context: Context,
    /** 경로 동의가 필요한 세션에 쓸 동의 화면. 화면이 들고 있다가 넘겨준다. */
    requestRouteConsent: suspend (sessionId: String) -> List<ExternalWorkoutPoint>?,
) :
    MviViewModel<Nothing, ImportRecordsProgressState, ImportRecordsProgressEffect>(
        initialState = ImportRecordsProgressState(),
    ) {
    private val importer = HealthConnectImporter(context, requestRouteConsent)

    init {
        // 진행 상황은 가져오기 로직이 전역 홀더로 내보낸다 — 그대로 화면 상태로 옮긴다. 홀더에
        // 지난 실행의 마지막 값이 남아 있을 수 있지만, 바로 아래 가져오기가 첫 단계를 내보내며
        // 덮는다(이동은 이 값이 아니라 Effect가 결정한다 — Effect 주석 참고).
        viewModelScope.launch {
            ImportProgressHolder.state.collect { progress -> setState { copy(progress = progress) } }
        }

        viewModelScope.launch {
            // Health Connect 조회와 /match 요청이 둘 다 블로킹이라 IO로 넘긴다.
            val result = withContext(Dispatchers.IO) { importer.importNewWorkouts() }
            sendEffect(
                // importNewWorkouts()는 끝까지 돌았으면 Finished를, 시작조차 못 했으면
                // Unavailable을 돌려준다 — 그 외 단계값이 반환될 일은 없다.
                if (result is ImportProgress.Finished) {
                    ImportRecordsProgressEffect.Finished
                } else {
                    ImportRecordsProgressEffect.Unavailable
                },
            )
        }
    }

    override suspend fun handleIntent(intent: Nothing) = Unit
}
