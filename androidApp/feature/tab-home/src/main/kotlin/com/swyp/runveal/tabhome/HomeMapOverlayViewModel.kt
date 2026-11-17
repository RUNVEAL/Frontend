package com.swyp.runveal.tabhome

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.swyp.runveal.devicestate.currentLocationOrNull
import com.swyp.runveal.network.dong.CurrentDongHolder
import com.swyp.runveal.network.dong.DongApi
import com.swyp.runveal.network.dong.DongResult
import com.swyp.runveal.tabhome.contract.HomeMapOverlayState
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.launch

private const val TAG = "HomeMapOverlay"

class HomeMapOverlayViewModel : MviViewModel<Nothing, HomeMapOverlayState, Nothing>(
    initialState = HomeMapOverlayState(),
) {
    private val dongApi = DongApi()

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
            val location = context.currentLocationOrNull()
            if (location == null) {
                Log.i(TAG, "현재 위치를 받지 못해 동 조회를 건너뛴다(권한 없음 또는 위치 비활성)")
                return@launch
            }

            when (val result = dongApi.fetchDong(lat = location.lat, lon = location.lon)) {
                is DongResult.Success -> {
                    // 시도를 뺀 이름을 쓴다 — 한 줄짜리 자리라 "충청남도 천안시서북구 부성2동"은
                    // 길어서 잘린다(DongResponse.shortName).
                    setState { copy(dongName = result.dong.shortName, dongCode = result.dong.code) }
                    // 줌아웃 요약 카드는 다른 모듈(feature:home)이라 상태를 직접 못 본다 — 홀더로 넘긴다.
                    CurrentDongHolder.publish(result.dong)
                }

                DongResult.OutOfServiceArea -> Log.i(TAG, "서비스 지역 밖 — 동 이름 없이 둔다")

                is DongResult.Failure -> Log.w(TAG, "동 조회 실패 — ${result.debugMessage}")
            }
        }
    }
}
