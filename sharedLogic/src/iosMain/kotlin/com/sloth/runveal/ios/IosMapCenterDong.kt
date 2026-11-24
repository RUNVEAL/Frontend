package com.sloth.runveal.ios

import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult
import com.sloth.runveal.network.dong.MapCenterDongHolder

/**
 * 지도 가운데 보이는 행정동 — 홈 줌인·줌아웃 패널이 이름과 탐험률을 여기서 가져온다.
 *
 * [IosCurrentDong]과 달리 `CurrentDongHolder`가 아니라 [MapCenterDongHolder]에 올린다. 그쪽은
 * "지금 내가 서 있는 동"이고 탐험률 집계·러닝 기록이 기준으로 쓰므로, 지도를 끌 때마다 바뀌는
 * 값을 섞으면 안 된다.
 *
 * 서비스 지역 밖(바다·해외)이나 실패면 null이고 홀더도 비운다 — 옛 이름을 그대로 두면 지도를
 * 바다로 끌어도 엉뚱한 동네 이름이 남는다.
 */
object IosMapCenterDong {

    private val dongApi = DongApi()

    @Throws(Exception::class) // 없으면 Swift의 `try?` 대신 앱이 종료된다 — IosCurrentDong 주석 참고.
    suspend fun fetch(lat: Double, lon: Double): CurrentDong? =
        when (val result = dongApi.fetchDong(lat = lat, lon = lon)) {
            is DongResult.Success -> {
                // 하단 요약 카드는 다른 화면이라 홀더로 넘긴다(Android와 같은 경로).
                MapCenterDongHolder.publish(result.dong)
                // 행정동만 쓴다 — 패널은 한 줄짜리 자리라 시군구까지 넣으면 잘린다.
                CurrentDong(name = result.dong.dongOnly, code = result.dong.code)
            }

            DongResult.OutOfServiceArea, is DongResult.Failure -> {
                MapCenterDongHolder.clear()
                null
            }
        }
}
