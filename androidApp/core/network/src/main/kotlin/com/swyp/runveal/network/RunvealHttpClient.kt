package com.swyp.runveal.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Runveal 서버([NetworkConfig.BASE_URL]) 호출용 공용 Ktor [HttpClient]. 엔드포인트별 API 클래스는
 * 이 인스턴스를 재사용한다 — `ValhallaClient`처럼 호출부마다 새로 만들지 않는다.
 */
object RunvealHttpClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val instance: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(Logging) { level = LogLevel.INFO }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 30_000
            }
        }
    }
}
