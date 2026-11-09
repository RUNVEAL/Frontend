package com.swyp.runveal.valhalla

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** `feature-valhalla-matching.md` "HTTP 클라이언트: ValhallaHttpClient" — 원안(Retrofit)을 KMP에 맞게
 * Ktor로 재작성(`MatchedEdge.kt`의 주석 "Ktor 재작성 예정"이 바로 이 부분). `encodeDefaults = true`가
 * 없으면 costing/shape_match처럼 기본값인 필드가 요청 JSON에서 빠지고, Valhalla가
 * "No edge/node costing provided"로 요청을 전부 거부한다(실기기에서 확인된 버그 — 주석으로 남겨둠).
 */
class ValhallaClient(private val baseUrl: String = ValhallaConfig.BASE_URL) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
    }

    suspend fun traceAttributes(request: TraceRequest): TraceResponse {
        val url = baseUrl.trimEnd('/') + "/trace_attributes"
        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
