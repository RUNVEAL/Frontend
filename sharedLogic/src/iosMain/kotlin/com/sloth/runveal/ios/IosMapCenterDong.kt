package com.sloth.runveal.ios

import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult

/**
 * 지도 가운데 보이는 행정동 — 홈의 **좌측 상단 위치 라벨**과 줌인 패널이 이름을 여기서 가져온다.
 *
 * [IosCurrentDong]과 달리 홀더에 올리지 않는다 — 이 값을 보는 건 지도를 띄운 화면 하나뿐이다.
 * 하단 요약 카드는 지도를 움직여도 바뀌지 않으므로(기록 기준 "내 동네" 또는 "현위치") 이 값을
 * 쓰지 않는다.
 *
 * 서비스 지역 밖(바다·해외)이나 실패면 null이다 — 옛 이름을 그대로 두면 지도를 바다로 끌어도
 * 엉뚱한 동네 이름이 남는다.
 */
object IosMapCenterDong {

    private val dongApi = DongApi()

    @Throws(Exception::class) // 없으면 Swift의 `try?` 대신 앱이 종료된다 — IosCurrentDong 주석 참고.
    suspend fun fetch(lat: Double, lon: Double): CurrentDong? =
        when (val result = dongApi.fetchDong(lat = lat, lon = lon)) {
            // 행정동만 쓴다 — 패널은 한 줄짜리 자리라 시군구까지 넣으면 잘린다.
            is DongResult.Success -> CurrentDong(name = result.dong.dongOnly, code = result.dong.code)

            DongResult.OutOfServiceArea, is DongResult.Failure -> null
        }
}
