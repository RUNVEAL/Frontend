package com.sloth.runveal.tabhome

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.sloth.runveal.devicestate.currentLocationOrNull
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.dong.CurrentDongHolder
import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult
import com.sloth.runveal.network.dong.MapCenterDongHolder
import com.sloth.runveal.tabhome.contract.HomeMapOverlayState
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "HomeMapOverlay"

class HomeMapOverlayViewModel : MviViewModel<Nothing, HomeMapOverlayState, Nothing>(
    initialState = HomeMapOverlayState(),
) {
    private val dongApi = DongApi()

    /** 진행 중인 지도 중심 동 조회 — 카메라가 또 멈추면 앞선 요청을 버린다. */
    private var centerDongJob: Job? = null

    /** 마지막으로 조회한 지도 중심 — 이 자리에서 [CENTER_DONG_REFRESH_DISTANCE_M]을 넘게
     * 움직였을 때만 다시 묻는다. */
    private var lastCenterQueried: LatLon? = null

    override suspend fun handleIntent(intent: Nothing) = Unit

    /**
     * 현재 위치의 행정동을 받아 온다. 화면이 Context를 쥐고 있어 호출부에서 넘겨받는다.
     *
     * 동 판정은 서버에만 맡긴다 — 로컬 경계 polygon으로도 할 수 있지만, 양쪽 기준이 어긋나면 같은
     * 자리에서 지도(정복률)와 라벨이 다른 동을 가리킨다. 실패하면 상태를 비워 둬서 화면이 대체
     * 문구를 쓰게 한다 — 위치 권한이 없거나 서비스 지역 밖인 건 흔한 상황이라 오류로 다루지 않는다.
     */
    fun loadCurrentDong(context: Context) {
        viewModelScope.launch {
            val location = awaitCurrentLocation(context)
            if (location == null) {
                Log.i(TAG, "현재 위치를 받지 못해 동 조회를 건너뛴다(권한 없음 또는 위치 비활성)")
                return@launch
            }

            when (val result = dongApi.fetchDong(lat = location.lat, lon = location.lon)) {
                is DongResult.Success -> {
                    // 행정동만 쓴다 — 지도 위 한 줄짜리 자리라 "충청남도 천안시서북구 부성2동"은
                    // 길어서 잘리고, 줌인 헤더는 "내 동네"가 앞에 붙어 더 길어진다.
                    setState { copy(dongName = result.dong.dongOnly, dongCode = result.dong.code) }
                    // 줌아웃 요약 카드는 다른 모듈(feature:home)이라 상태를 직접 못 본다 — 홀더로 넘긴다.
                    CurrentDongHolder.publish(result.dong)
                }

                DongResult.OutOfServiceArea -> {
                    Log.i(TAG, "서비스 지역 밖 — 동 이름 없이 둔다")
                    CurrentDongHolder.publishUnavailable()
                }

                is DongResult.Failure -> {
                    Log.w(TAG, "동 조회 실패 — ${result.debugMessage}")
                    // 홈이 "데이터를 받지 못했어요" 배너를 띄울 수 있게 알린다 — 권한 없음·지역 밖과
                    // 달리 이건 사용자가 다시 시도해 볼 만한 실패다.
                    CurrentDongHolder.publishFailure()
                }
            }
        }
    }

    /**
     * 지도 가운데 보이는 행정동을 받아 온다 — 카메라가 멈출 때마다 화면이 부른다.
     *
     * **조금 움직인 건 무시한다.** 멈출 때마다 묻게 두면 지도를 조금씩 미는 동안 요청이 쏟아지는데,
     * 줌아웃 화면에서 몇백 미터는 거의 같은 동이라 결과도 대개 똑같다. 이전에 물어본 자리에서
     * [CENTER_DONG_REFRESH_DISTANCE_M]을 넘게 벗어났을 때만 다시 묻는다.
     *
     * 서비스 지역 밖(바다·해외)이나 실패면 이름을 비운다 — 옛 동 이름을 그대로 두면 지도를 바다로
     * 끌어도 엉뚱한 동네 이름이 남는다.
     */
    fun loadCenterDong(center: LatLon) {
        val last = lastCenterQueried
        if (last != null && GeoMath.distanceMeters(last, center) < CENTER_DONG_REFRESH_DISTANCE_M) return
        lastCenterQueried = center

        centerDongJob?.cancel()
        centerDongJob = viewModelScope.launch {
            when (val result = dongApi.fetchDong(lat = center.lat, lon = center.lon)) {
                is DongResult.Success -> {
                    setState { copy(centerDongName = result.dong.dongOnly, centerDongCode = result.dong.code) }
                    // 하단 요약 카드는 다른 모듈(feature:home)이라 상태를 직접 못 본다 — 홀더로 넘긴다.
                    MapCenterDongHolder.publish(result.dong)
                }

                DongResult.OutOfServiceArea -> {
                    Log.i(TAG, "지도 중심이 서비스 지역 밖 — 이름을 비운다")
                    clearCenterDong()
                }

                is DongResult.Failure -> {
                    Log.w(TAG, "지도 중심 동 조회 실패 — ${result.debugMessage}")
                    // 다음에 같은 자리에서 다시 시도할 수 있게 "물어본 자리"를 되돌린다.
                    lastCenterQueried = last
                    clearCenterDong()
                }
            }
        }
    }

    private fun clearCenterDong() {
        setState { copy(centerDongName = null, centerDongCode = null) }
        MapCenterDongHolder.clear()
    }

    /**
     * 현재 위치를 받는다. 권한을 막 허용한 직후에는 위치 공급자가 아직 덜 깨어나 첫 요청이 그냥
     * 빈손으로 돌아오는 일이 있어서, 몇 번 더 기다려 본다 — 한 번 만에 포기하면 라벨이 "현재 위치"
     * 에 멈춰 버린다.
     *
     * 권한이 없으면 첫 요청이 바로 null로 떨어지는데, 그 경우에도 여기서 몇 초를 더 쓰는 건 상관
     * 없다 — 권한을 켜면 홈이 다시 조회를 시키고([CurrentDongHolder.requestRetry]) 그때 이 루프는
     * 취소된다.
     */
    private suspend fun awaitCurrentLocation(context: Context): LatLon? {
        repeat(LOCATION_ATTEMPTS) { attempt ->
            context.currentLocationOrNull()?.let { return it }
            if (attempt < LOCATION_ATTEMPTS - 1) delay(LOCATION_RETRY_DELAY_MS)
        }
        return null
    }
}

/**
 * 지도 중심 동을 다시 물어볼 최소 이동 거리(m). 줌아웃 화면에서 이 정도는 거의 같은 동이라,
 * 넘지 않으면 묻지 않는다. 줄이면 더 민감해지고 요청이 늘어난다.
 */
private const val CENTER_DONG_REFRESH_DISTANCE_M = 300.0

/** 현재 위치를 받아 볼 횟수 — 권한 허용 직후 공급자가 깨어나는 데 걸리는 시간만 메우면 된다. */
private const val LOCATION_ATTEMPTS = 3
private const val LOCATION_RETRY_DELAY_MS = 2000L
