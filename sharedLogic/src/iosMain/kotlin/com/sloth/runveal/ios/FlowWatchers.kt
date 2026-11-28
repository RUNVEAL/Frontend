package com.sloth.runveal.ios

import com.sloth.runveal.auth.state.AuthState
import com.sloth.runveal.auth.state.AuthViewModel
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.meta.MetaHolder
import com.sloth.runveal.network.meta.MetaResponse
import com.sloth.runveal.recording.TrackRecorder
import com.sloth.runveal.result.LiveMatchHolder
import com.sloth.runveal.result.LiveNewRoadHolder
import com.sloth.runveal.result.TrackResult
import com.sloth.runveal.recording.GpsSignalHolder
import com.sloth.runveal.result.TrackResultHolder
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

    /** 매칭 선은 GPS와 동떨어진 부분을 잘라낸 **조각 목록**이다(`MatchedPathFilter`) — 하나로 이어
     * 붙이면 잘라낸 구간이 직선으로 되살아나므로 조각 그대로 넘긴다. */
    fun watchLiveMatch(onEach: (List<List<LatLon>>?) -> Unit): FlowSubscription =
        LiveMatchHolder.state.watch(onEach)

    fun watchLiveNewRoadMeters(onEach: (Double) -> Unit): FlowSubscription =
        LiveNewRoadHolder.state.watch(onEach)

    /** 러닝 중 GPS 신호가 약한지([GpsSignalHolder]) — 화면이 안내 배너를 띄울지 정한다. */
    fun watchGpsSignalWeak(onEach: (Boolean) -> Unit): FlowSubscription =
        GpsSignalHolder.isWeak.watch(onEach)

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
