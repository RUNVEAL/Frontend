package com.sloth.runveal.network.routes

import com.sloth.runveal.network.NetworkConfig
import com.sloth.runveal.network.RunvealHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** `POST /api/v1/routes` — `docs/api/API.md` §3.3. 출발/도착·거리·필터·달린 길 번호로 코스를 0~3개
 * 받는다. 위치 API라 인증 토큰은 안 붙인다(§1 핵심 원칙 7). */
class RoutesApi(
    private val client: HttpClient = RunvealHttpClient.instance,
    private val baseUrl: String = NetworkConfig.BASE_URL,
) {
    suspend fun fetchRoutes(request: RoutesRequest): RoutesResponse =
        client.post(baseUrl + "/api/v1/routes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
