package com.sloth.runveal.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.health.connect.client.contracts.ExerciseRouteRequestContract
import androidx.health.connect.client.records.ExerciseRoute
import com.sloth.runveal.external.ExternalWorkoutPoint
import kotlinx.coroutines.CompletableDeferred

/**
 * 운동 **세션 하나**의 경로를 읽을 동의를 받는다.
 *
 * 다른 앱(삼성헬스, 가민 등)이 기록한 경로는 `ExerciseRouteResult.ConsentRequired`로 와서 좌표가
 * 비어 있다. 그때 이 화면을 띄워야 그 세션의 경로를 받을 수 있다 —
 * [HealthConnectPermissions.READ_EXERCISE_ROUTES]는 일반 권한 요청 화면으로 받을 수 없어서
 * 이 길뿐이다(그 주석 참고).
 *
 * 사용자가 "항상 허용"을 고르면 그 뒤로는 `Data`로 바로 와서 이 화면이 다시 뜨지 않는다. 그래서
 * 기록이 수십 건이어도 실제로는 보통 한 번만 뜬다.
 *
 * **화면이 떠 있어야 한다.** 백그라운드에서 다른 앱의 경로를 읽으면 "항상 허용" 상태여도
 * `ConsentRequired`가 돌아온다 — 가져오기를 WorkManager 같은 데로 옮기면 안 되는 이유다.
 */
@Stable
class ExerciseRouteConsentRequester internal constructor() {
    internal var launcher: ActivityResultLauncher<String>? = null
    private var pending: CompletableDeferred<ExerciseRoute?>? = null

    /**
     * [sessionId] 세션의 경로 동의 화면을 띄우고 결과를 기다린다. 사용자가 거부했거나 화면을 띄울
     * 수 없으면 null — 호출부는 그 기록을 건너뛴다.
     */
    suspend fun request(sessionId: String): List<ExternalWorkoutPoint>? {
        val launcher = launcher ?: return null
        // 앞선 요청이 결과 없이 남아 있으면 끊는다 — 하나씩만 띄운다.
        pending?.complete(null)

        val deferred = CompletableDeferred<ExerciseRoute?>()
        pending = deferred
        launcher.launch(sessionId)

        return deferred.await()?.route?.map {
            ExternalWorkoutPoint(
                lat = it.latitude,
                lon = it.longitude,
                timeEpochMs = it.time.toEpochMilli(),
            )
        }
    }

    internal fun deliver(route: ExerciseRoute?) {
        pending?.complete(route)
        pending = null
    }
}

@Composable
fun rememberExerciseRouteConsentRequester(): ExerciseRouteConsentRequester {
    val requester = remember { ExerciseRouteConsentRequester() }
    val launcher = rememberLauncherForActivityResult(ExerciseRouteRequestContract()) { route ->
        requester.deliver(route)
    }
    // 런처는 컴포지션마다 새로 만들어질 수 있어 생성자로 넘기지 않고 여기서 갈아 끼운다.
    SideEffect { requester.launcher = launcher }
    return requester
}
