package com.sloth.runveal.network.error

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

/**
 * 서버 공통 에러 응답(`docs/api/API.md` §5.4). 모든 에러가 같은 모양으로 오고, **앱은 [code]로만
 * 분기한다** — [message]는 개발자용 영어 한 줄이라 바뀔 수 있고 화면에 그대로 쓰면 안 된다.
 *
 * RFC 9457 형식이라 `type`·`title`·`detail`도 같이 오지만 쓰지 않는다(`ignoreUnknownKeys`로 무시).
 */
@Serializable
data class RunvealErrorResponse(
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val requestId: String? = null,
    /** 400일 때만. 여러 개가 틀려도 첫 번째 1건만 온다. */
    val field: String? = null,
    val reason: String? = null,
)

/** `docs/api/API.md` §5.5의 에러 코드. 문자열 비교 대신 이 상수를 쓴다. */
object RunvealErrorCode {
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val MAP_VERSION_OUTDATED = "MAP_VERSION_OUTDATED"
    const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
    const val TOO_MANY_POINTS = "TOO_MANY_POINTS"
    const val TOO_MANY_SEGMENTS = "TOO_MANY_SEGMENTS"
    const val OUT_OF_SERVICE_AREA = "OUT_OF_SERVICE_AREA"
    const val MATCH_FAILED = "MATCH_FAILED"
    const val RATE_LIMITED = "RATE_LIMITED"
}

/**
 * 실패 응답의 `code`. 본문이 비었거나 형식이 다르면 null — 그때는 상태 코드만 보고 판단해야 한다.
 * 본문을 한 번만 읽을 수 있으므로(스트림) 실패 처리 경로에서 한 번만 호출한다.
 */
suspend fun HttpResponse.errorCodeOrNull(): String? =
    runCatching { body<RunvealErrorResponse>().code }.getOrNull()
