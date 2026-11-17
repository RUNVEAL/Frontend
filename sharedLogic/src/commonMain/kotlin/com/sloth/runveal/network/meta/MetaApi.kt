package com.sloth.runveal.network.meta

import com.sloth.runveal.network.NetworkConfig
import com.sloth.runveal.network.RunvealHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** `GET /api/v1/meta` — `docs/api/API.md` §3.1. 서버가 지원하는 지도 버전·요청 상한·필터 목록을 받는다.
 * 위치 API라 인증 토큰은 안 붙인다(§1 핵심 원칙 7). */
class MetaApi(
    private val client: HttpClient = RunvealHttpClient.instance,
    private val baseUrl: String = NetworkConfig.BASE_URL,
) {
    suspend fun fetchMeta(): MetaResponse =
        client.get(baseUrl + "/api/v1/meta").body()
}
