package com.swyp.runveal.network.dong

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
) {
    /**
     * 시도를 뺀 이름 — 예: "서울특별시 종로구 사직동" → "종로구 사직동".
     *
     * 지도 위 "현재 위치" 자리는 한 줄짜리라 시도까지 넣으면 길어서 잘린다. 지금 어디인지는
     * 시군구부터면 충분히 알아본다.
     *
     * 세종특별자치시처럼 **시군구가 없는 곳**(토막 2개)은 그대로 둔다 — 앞을 떼면 "한솔동"만
     * 남아 어느 시의 동인지 알 수 없다.
     */
    val shortName: String
        get() {
            val parts = name.split(" ").filter { it.isNotBlank() }
            return if (parts.size >= 3) parts.drop(1).joinToString(" ") else name
        }
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
