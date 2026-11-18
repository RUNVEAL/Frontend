package com.sloth.runveal.ios

import com.sloth.runveal.network.dong.CurrentDongHolder
import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult

/**
 * 현재 위치의 행정동 — Swift가 sealed 계층(`DongResult`)을 직접 다루지 않게 평평하게 만든 결과.
 *
 * 못 받은 경우(위치 권한 없음, 서비스 지역 밖, 서버 실패)는 전부 `null` 하나로 합친다. 화면이
 * 하는 일이 "이름이 있으면 보여주고 없으면 대체 문구"로 같기 때문이다.
 */
data class CurrentDong(
    /** 행정동 이름만("부성2동"). */
    val name: String,
    /** 행정동 코드(`adm_cd2`) — 탐험률·거리를 꺼내는 키다. */
    val code: String,
)

/**
 * 현재 좌표의 행정동을 서버에 묻는다 — Android `HomeMapOverlayViewModel.loadCurrentDong()`에 해당.
 *
 * 동 판정은 서버에만 맡긴다 — 로컬 경계 polygon으로도 할 수 있지만, 양쪽 기준이 어긋나면 같은
 * 자리에서 지도(탐험률)와 라벨이 다른 동을 가리킨다.
 *
 * 실패하면 null — 위치 권한이 없거나 서비스 지역 밖인 건 흔한 상황이라 오류로 다루지 않는다.
 */
object IosCurrentDong {

    private val dongApi = DongApi()

    suspend fun fetch(lat: Double, lon: Double): CurrentDong? =
        when (val result = dongApi.fetchDong(lat = lat, lon = lon)) {
            is DongResult.Success -> {
                // 줌아웃 요약 카드도 같은 값을 쓰도록 홀더에 넘긴다(Android와 같은 경로).
                CurrentDongHolder.publish(result.dong)
                // 행정동만 쓴다 — 지도 위 한 줄짜리 자리라 "충청남도 천안시서북구 부성2동"은
                // 길어서 잘리고, 줌인 헤더는 "내 동네"가 앞에 붙어 더 길어진다.
                CurrentDong(name = result.dong.dongOnly, code = result.dong.code)
            }

            DongResult.OutOfServiceArea -> null
            is DongResult.Failure -> null
        }
}
