package com.sloth.runveal.network.dong

import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/dong` 응답 — 좌표가 속한 행정동.
 *
 * [code]는 정복률 집계(`explored_segment.adm_cd2`)와 **같은 10자리 행정동 코드**라, 받은 값을 그대로
 * 매칭하면 된다. 로컬 경계 polygon으로 동을 다시 판정하면 안 된다 — 동 코드 체계를 서버 한 곳에서만
 * 관리하기로 했고, 양쪽 판정이 어긋나면 같은 자리에서 지도와 라벨이 다른 동을 가리킨다.
 */
@Serializable
data class DongResponse(
    val code: String,
    /** 시군구 코드 5자리. 구간 인덱스를 구 단위로 들여올 때 쓴다(`RoadSegmentIndexImporter`). */
    val gu: String,
    /** 사람이 읽는 전체 이름 — 예: "서울특별시 종로구 사직동". 화면에는 [shortName]을 쓴다. */
    val name: String,
    /**
     * 좌표 근처의 도로명 — 예: "한보라2로". **이름 있는 도로가 없으면 null이다**(작은 골목·샛길).
     *
     * 화면에는 [roadOrDongName]을 쓴다 — null을 그대로 두면 빈칸이 된다. 기본값을 둔 이유는 이
     * 필드가 나중에 추가됐기 때문이다: 서버가 안 내려주던 때의 응답(또는 테스트 픽스처)도 그대로
     * 읽혀야 한다.
     */
    val road: String? = null,
) {
    /**
     * 시도를 뺀 이름 — 예: "서울특별시 종로구 사직동" → "종로구 사직동".
     *
     * 지도 위 "현재 위치" 자리는 한 줄짜리라 시도까지 넣으면 길어서 잘린다. 지금 어디인지는
     * 시군구부터면 충분히 알아본다.
     *
     * 세종은 시군구 단계가 없지만 서버가 "세종특별자치시 세종시 보람동"으로 채워 내려줘서 같은
     * 규칙이 그대로 맞는다. 혹시 토막이 2개인 이름이 오면 앞을 떼지 않는다 — 떼면 동 이름만 남아
     * 어느 시인지 알 수 없다.
     */
    val shortName: String
        get() {
            val parts = name.split(" ").filter { it.isNotBlank() }
            return if (parts.size >= 3) parts.drop(1).joinToString(" ") else name
        }

    /**
     * 행정동만 — 예: "충청남도 천안시서북구 부성1동" → "부성1동".
     *
     * 홈 요약 카드처럼 "내 동네" 꼬리표가 바로 뒤에 붙는 자리에 쓴다. 거기선 어느 구인지까지
     * 적을 자리도 없고, 내 동네라는 게 이미 드러나서 동 이름만으로 충분하다.
     */
    val dongOnly: String
        get() = name.split(" ").lastOrNull { it.isNotBlank() } ?: name

    /**
     * 고른 자리를 가리키는 이름 — 도로명([road])이 있으면 그걸, 없으면 [shortName]을 쓴다.
     *
     * 코스 만들기의 출발지·도착지 선택이 이 값을 보여준다. 길 위의 한 점을 고르는 일이라 "한보라2로"
     * 처럼 도로명이 가장 정확한데, 이름 있는 도로가 없는 골목이면 서버가 [road]를 비워 보낸다 —
     * 그때 빈칸을 두면 어디를 고른 건지 아예 알 수 없으니 동 이름으로 메운다.
     *
     * [dongOnly]가 아니라 [shortName]으로 메운다 — 출발지와 도착지가 서로 다른 구일 수 있어서
     * 동 이름만으로는 어느 쪽을 고른 건지 가려지지 않는다.
     *
     * 빈 문자열도 없는 것으로 본다 — 서버가 도로명을 못 찾았을 때 null 대신 ""를 줄 여지가 있고,
     * 그 경우까지 빈칸으로 새면 같은 증상이 된다.
     */
    val roadOrDongName: String
        get() = road?.takeIf { it.isNotBlank() } ?: shortName
}

/** `GET /api/v1/dong` 결과. 실패를 예외로 던지지 않고 타입으로 나눠, 호출부가 "서비스 지역 밖"과
 * 일시적 오류를 구분해 다르게 보여줄 수 있게 한다(`docs/api/API.md` §5.5). */
sealed interface DongResult {
    data class Success(val dong: DongResponse) : DongResult

    /** 422 `OUT_OF_SERVICE_AREA` — 바다·해외 등. 재시도해도 결과가 같다. */
    data object OutOfServiceArea : DongResult

    /** 그 밖의 실패(네트워크, 5xx, 400 등). [debugMessage]는 로그용이고 화면에 그대로 쓰지 않는다. */
    data class Failure(val debugMessage: String) : DongResult
}
