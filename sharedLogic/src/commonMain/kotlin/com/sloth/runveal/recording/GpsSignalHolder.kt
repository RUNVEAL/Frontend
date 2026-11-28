package com.sloth.runveal.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 러닝 중 GPS 신호가 약한지. 화면이 이 값으로 "GPS 신호가 약해요" 안내를 띄운다.
 *
 * **판정 기준은 "쓸 만한 점이 마지막으로 들어온 지 얼마나 됐나"다.** 정확도가 [TrackFilter]의 기준
 * 안에 드는 점이 오면 바로 괜찮은 것으로 보고, 그 뒤로 [WEAK_AFTER_MS]가 넘도록 그런 점이 없으면
 * 약한 것으로 본다. 두 가지를 한 기준으로 덮는다 — 정확도가 나빠 점이 계속 버려지는 경우와, 신호가
 * 끊겨 점 자체가 안 오는 경우.
 *
 * **"버려진 점"을 세지 않는 이유**: [TrackFilter]는 제자리에 서 있을 때도 거리 기준으로 점을
 * 버린다([DropReason.DISTANCE]). 그걸 신호 약함으로 보면 신호등에서 기다릴 때마다 안내가 뜬다.
 *
 * 점이 아예 안 오는 경우는 [onRawPoint]로는 알 수 없어서, 화면이 1초마다 [evaluate]를 불러 준다.
 */
object GpsSignalHolder {
    private val _isWeak = MutableStateFlow(false)
    val isWeak: StateFlow<Boolean> = _isWeak.asStateFlow()

    /** 정확도가 쓸 만했던 마지막 점의 시각. 러닝 시작 시각으로 초기화해서, 첫 측위를 기다리는
     * 동안에는 안내가 뜨지 않게 한다(실내에서 시작하면 몇 초는 걸린다). */
    private var lastGoodAtEpochMs: Long? = null

    /** 러닝을 시작할 때 — 이전 러닝의 상태가 남지 않게 비우고 기준 시각을 잡는다. */
    fun start(startedAtEpochMs: Long) {
        lastGoodAtEpochMs = startedAtEpochMs
        _isWeak.value = false
    }

    /** 점을 받을 때마다. 쓸 만한 점이면 그 자리에서 안내를 내린다 — 신호가 돌아왔는데 다음 1초
     * 틱까지 안내가 남아 있으면 고장으로 보인다. */
    fun onRawPoint(point: TrackPoint) {
        if (point.accuracyMeters <= TrackFilter.MAX_ACCURACY_METERS) {
            lastGoodAtEpochMs = point.timeEpochMs
            _isWeak.value = false
        } else {
            evaluate(point.timeEpochMs)
        }
    }

    /** 화면이 1초마다 불러 준다. 점이 **안 오는** 동안에도 시간은 흐르므로 이쪽에서만 알 수 있다. */
    fun evaluate(nowEpochMs: Long) {
        val lastGood = lastGoodAtEpochMs ?: return
        _isWeak.value = nowEpochMs - lastGood > WEAK_AFTER_MS
    }

    /** 러닝이 끝났을 때 — 다음 화면까지 안내가 따라가지 않게 비운다. */
    fun clear() {
        lastGoodAtEpochMs = null
        _isWeak.value = false
    }
}

/**
 * 쓸 만한 점이 이 시간을 넘도록 없으면 신호가 약한 것으로 본다.
 *
 * 위치 요청 간격이 5초라, 세 번 놓친 정도다. 더 짧게 잡으면 한두 번 튄 것만으로 안내가 켜졌다
 * 꺼졌다 한다.
 */
private const val WEAK_AFTER_MS = 15_000L
