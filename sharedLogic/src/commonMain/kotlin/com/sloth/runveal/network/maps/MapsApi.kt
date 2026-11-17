package com.sloth.runveal.network.maps

import com.sloth.runveal.network.NetworkConfig
import com.sloth.runveal.network.RunvealHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** `GET /maps/{mapVersion}/...` — `docs/api/API.md` §3.1.2/§3.2/§3.7. 지도 버전이 바뀌면 manifest로
 * 파일 목록을 받고, 파일을 하나씩 받아 로컬 지도를 교체한다. 같은 버전의 파일은 안 바뀌므로 영구
 * 캐시해도 된다. */
class MapsApi(
    private val client: HttpClient = RunvealHttpClient.instance,
    private val baseUrl: String = NetworkConfig.BASE_URL,
) {
    suspend fun fetchManifest(mapVersion: String): ManifestResponse =
        client.get("$baseUrl/maps/$mapVersion/manifest.json").body()

    /** `manifest.files`에 있는 파일 이름을 그대로 넘긴다(`segments.bin`, `index.geojson` 등).
     * 바이너리(`segments.bin`)·텍스트(`*.json`, `*.geojson`) 파일 모두 원본 바이트로 받는다 — 디코딩은
     * 호출부가 파일 종류에 맞게 한다. */
    suspend fun fetchFile(mapVersion: String, fileName: String): ByteArray =
        client.get("$baseUrl/maps/$mapVersion/$fileName").body()
}
