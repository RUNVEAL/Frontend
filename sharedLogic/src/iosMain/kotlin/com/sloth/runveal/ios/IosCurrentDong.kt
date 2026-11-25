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
/**
 * [IosCurrentDong.fetch] 결과 — 동을 못 받은 이유를 호출부가 가릴 수 있게 둘로 나눠 담는다.
 *
 * [dong]만으로는 **서버에서 못 받은 것**과 **서비스 지역 밖**이 구분되지 않는다(둘 다 null). 홈은
 * 전자일 때만 "현재 위치 정보를 받지 못했어요" 배너를 띄워야 하므로 [loadFailed]를 따로 본다.
 */
data class CurrentDongResult(
    val dong: CurrentDong?,
    /** 서버에서 못 받았는지(오프라인·서버 꺼짐·타임아웃). 서비스 지역 밖은 false다 — 정상 상황이다. */
    val loadFailed: Boolean,
)

object IosCurrentDong {

    private val dongApi = DongApi()

    @Throws(Exception::class) // 없으면 Swift의 `try?` 대신 앱이 종료된다 — IosMeta.fetchAndPublish 주석.
    suspend fun fetch(lat: Double, lon: Double): CurrentDongResult =
        when (val result = dongApi.fetchDong(lat = lat, lon = lon)) {
            is DongResult.Success -> {
                // 줌아웃 요약 카드도 같은 값을 쓰도록 홀더에 넘긴다(Android와 같은 경로).
                CurrentDongHolder.publish(result.dong)
                CurrentDongResult(
                    // 행정동만 쓴다 — 지도 위 한 줄짜리 자리라 "충청남도 천안시서북구 부성2동"은
                    // 길어서 잘리고, 줌인 헤더는 "내 동네"가 앞에 붙어 더 길어진다.
                    dong = CurrentDong(name = result.dong.dongOnly, code = result.dong.code),
                    loadFailed = false,
                )
            }

            // 바다·해외처럼 **정상적으로** 동이 없는 경우 — 실패 표시를 지운다.
            DongResult.OutOfServiceArea -> {
                CurrentDongHolder.publishUnavailable()
                CurrentDongResult(dong = null, loadFailed = false)
            }

            // 오프라인·서버 꺼짐 — 홈이 배너를 띄울 수 있게 실패를 남긴다.
            is DongResult.Failure -> {
                CurrentDongHolder.publishFailure()
                CurrentDongResult(dong = null, loadFailed = true)
            }
        }
}
