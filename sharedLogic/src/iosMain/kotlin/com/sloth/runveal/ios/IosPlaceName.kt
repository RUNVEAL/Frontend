package com.sloth.runveal.ios

import com.sloth.runveal.network.dong.DongApi
import com.sloth.runveal.network.dong.DongResult

/**
 * 지도에서 찍은 좌표의 자리 이름 — 코스 만들기의 출발지/도착지 선택이 쓴다.
 *
 * [IosCurrentDong]과 달리 `CurrentDongHolder`에 publish하지 **않는다.** 거기에 넣으면 코스를 만들며
 * 지도를 탭할 때마다 홈의 "내 동네" 라벨이 찍은 자리의 동으로 바뀐다 — 그 홀더는 "지금 내가 서 있는
 * 동" 하나만 들고 있어야 한다.
 *
 * 도로명이 있으면 도로명(예: "한보라2로"), 없으면 시도를 뺀 동 이름(예: "중구 명동")을 쓴다
 * (`roadOrDongName`) — 길 위의 한 점을 고르는 일이라 도로명이 가장 정확하고, 이름 있는 도로가 없는
 * 골목이면 서버가 도로명을 비워 보낸다. 메울 때 동 이름만 쓰지 않는 이유는 출발지와 도착지가 서로
 * 다른 구일 수 있어서다.
 *
 * 못 받은 경우(서비스 지역 밖, 서버 실패)는 전부 null 하나로 합친다 — 호출부가 하는 일이 "이름이
 * 있으면 보여주고 없으면 좌표를 그대로 둔다"로 같다.
 */
object IosPlaceName {

    private val dongApi = DongApi()

    suspend fun fetch(lat: Double, lon: Double): String? =
        when (val result = dongApi.fetchDong(lat = lat, lon = lon)) {
            is DongResult.Success -> result.dong.roadOrDongName
            DongResult.OutOfServiceArea -> null
            is DongResult.Failure -> null
        }
}
