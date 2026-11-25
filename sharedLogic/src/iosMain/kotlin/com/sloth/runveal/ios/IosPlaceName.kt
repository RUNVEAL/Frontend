package com.sloth.runveal.ios

import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult

/**
 * 지도에서 찍은 좌표의 행정동 이름 — 코스 만들기의 출발지/도착지 선택이 쓴다.
 *
 * [IosCurrentDong]과 달리 `CurrentDongHolder`에 publish하지 **않는다.** 거기에 넣으면 코스를 만들며
 * 지도를 탭할 때마다 홈의 "내 동네" 라벨이 찍은 자리의 동으로 바뀐다 — 그 홀더는 "지금 내가 서 있는
 * 동" 하나만 들고 있어야 한다.
 *
 * 시도를 뺀 이름(`shortName`, 예: "중구 명동")을 쓴다. 출발지와 도착지가 서로 다른 구일 수 있어
 * 동 이름만으로는 어디를 고른 건지 알 수 없다.
 *
 * 못 받은 경우(서비스 지역 밖, 서버 실패)는 전부 null 하나로 합친다 — 호출부가 하는 일이 "이름이
 * 있으면 보여주고 없으면 좌표를 그대로 둔다"로 같다.
 */
object IosPlaceName {

    private val dongApi = DongApi()

    suspend fun fetch(lat: Double, lon: Double): String? =
        when (val result = dongApi.fetchDong(lat = lat, lon = lon)) {
            is DongResult.Success -> result.dong.shortName
            DongResult.OutOfServiceArea -> null
            is DongResult.Failure -> null
        }
}
