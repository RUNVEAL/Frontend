package com.sloth.runveal.network.match

import com.sloth.runveal.network.NetworkConfig
import com.sloth.runveal.network.RunvealHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** `POST /api/v1/match` — `docs/api/API.md` §3.6/§3.7. 앱이 익명화한 GPS 점을 보내 달린 것으로
 * 인정된 구간 ID를 받는다. 위치 API라 인증 토큰은 안 붙인다(§1 핵심 원칙 7), 서버는 저장하지 않는다. */
class MatchApi(
    private val client: HttpClient = RunvealHttpClient.instance,
    private val baseUrl: String = NetworkConfig.BASE_URL,
) {
    suspend fun match(request: MatchRequest): MatchResponse =
        client.post(baseUrl + "/api/v1/match") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
