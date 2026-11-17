package com.swyp.runveal.ios

import com.swyp.runveal.auth.state.AuthState
import com.swyp.runveal.auth.state.AuthViewModel
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.network.meta.MetaResponse
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.result.LiveMatchHolder
import com.swyp.runveal.result.LiveNewRoadHolder
import com.swyp.runveal.result.TrackResult
import com.swyp.runveal.result.TrackResultHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * [RunvealFlowWatchers]로 시작한 구독을 끊는 핸들. Swift에서 화면이 사라질 때 [cancel]을 부른다.
 */
class FlowSubscription internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

/**
 * sharedLogic의 `StateFlow`를 Swift에서 구독할 수 있게 감싼 것.
 *
 * Kotlin/Native는 `Flow`를 Objective-C로 내보내지 않고 **제네릭 함수도 내보내지 않는다** — 그래서
 * `Flow<T>.watch(onEach)` 같은 범용 확장 대신, 필요한 플로우마다 구체 타입 함수를 하나씩 둔다.
 * 새 플로우를 iOS에서 구독해야 하면 여기에 같은 모양으로 하나 추가하면 된다.
 *
 * 콜백은 전부 메인 디스패처에서 불린다 — Swift 쪽은 SwiftUI 상태를 바로 갱신해도 된다.
 */
object RunvealFlowWatchers {

    /** [RecordingSnapshot]으로 평평하게 만들어 넘긴다 — Swift가 sealed 계층을 직접 다루지 않게. */
    fun watchRecording(onEach: (RecordingSnapshot) -> Unit): FlowSubscription =
        TrackRecorder.state.watch { state -> onEach(state.toSnapshot()) }

    fun watchTrackResult(onEach: (TrackResult?) -> Unit): FlowSubscription =
        TrackResultHolder.state.watch(onEach)

    fun watchMeta(onEach: (MetaResponse?) -> Unit): FlowSubscription =
        MetaHolder.state.watch(onEach)

    fun watchLiveMatch(onEach: (List<LatLon>?) -> Unit): FlowSubscription =
        LiveMatchHolder.state.watch(onEach)

    fun watchLiveNewRoadMeters(onEach: (Double) -> Unit): FlowSubscription =
        LiveNewRoadHolder.state.watch(onEach)

    fun watchAuthState(viewModel: AuthViewModel, onEach: (AuthState) -> Unit): FlowSubscription =
        viewModel.state.watch(onEach)

    private fun <T> Flow<T>.watch(onEach: (T) -> Unit): FlowSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            collect { value -> onEach(value) }
        }
        job.invokeOnCompletion { scope.cancel() }
        return FlowSubscription(job)
    }
}
