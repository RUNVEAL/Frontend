package com.swyp.runveal.network.dong

import com.swyp.runveal.network.NetworkConfig
import com.swyp.runveal.network.RunvealHttpClient
import com.swyp.runveal.network.error.RunvealErrorCode
import com.swyp.runveal.network.error.errorCodeOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * `GET /api/v1/dong?lat=&lng=` — 좌표가 속한 행정동. 위치 API라 인증 토큰은 안 붙인다
 * (`docs/api/API.md` §1 핵심 원칙 7).
 *
 * 쿼리 파라미터 이름이 `lng`다 — `/routes`의 `start`/`end`와 같은 계열이고 `lon`이 아니다(§5.2).
 */
class DongApi(
    private val client: HttpClient = RunvealHttpClient.instance,
    private val baseUrl: String = NetworkConfig.BASE_URL,
) {
    suspend fun fetchDong(lat: Double, lon: Double): DongResult {
        val response: HttpResponse = try {
            client.get(baseUrl + "/api/v1/dong") {
                parameter("lat", lat)
                parameter("lng", lon)
            }
        } catch (e: Exception) {
            return DongResult.Failure(e.message ?: "요청 실패")
        }

        if (response.status.isSuccess()) {
            return runCatching { DongResult.Success(response.body<DongResponse>()) }
                .getOrElse { DongResult.Failure(it.message ?: "응답 해석 실패") }
        }

        // 서비스 지역 밖은 "실패"가 아니라 정상적인 결과 중 하나라 따로 가려낸다. 상태 코드만 보지
        // 않고 code까지 확인한다 — 같은 422로 MATCH_FAILED 등이 올 수 있다(§5.5).
        val errorCode = response.errorCodeOrNull()
        if (response.status == HttpStatusCode.UnprocessableEntity &&
            errorCode == RunvealErrorCode.OUT_OF_SERVICE_AREA
        ) {
            return DongResult.OutOfServiceArea
        }
        return DongResult.Failure("${response.status.value} ${errorCode ?: "UNKNOWN"}")
    }
}
