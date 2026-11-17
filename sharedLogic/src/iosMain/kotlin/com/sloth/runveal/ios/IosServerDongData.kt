package com.sloth.runveal.ios

import com.sloth.runveal.network.meta.MetaHolder
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 서버 지도 파일에서 받은 동 경계와 동별 전체 도로 길이 — Android `DongAssetRepository.ServerDongMapData`. */
data class ServerDongMapData(
    val boundaryGeoJson: String,
    val totalLengthsByDong: Map<String, Double>,
    val mapVersion: String,
)

/**
 * 서버 `index.geojson`/`index.json`(`docs/api/API.md` §3.1.2)을 받아 온다 —
 * Android `DongAssetRepository.syncFromServer()`에 해당한다.
 *
 * **동 경계는 서버에서만 온다.** 2026-11부터 번들에서 `korea_dong.geojson`을 뺐다 — 번들 스냅샷은
 * 서버와 행정동 코드 체계가 약 13% 달라서(광주·전남 통합 `12` vs 분리 `29`/`46`, 인천 중구 재배정 등),
 * 그걸로 대신 그리면 완주율의 분자·분모와 지도 경계가 조용히 어긋난다. 못 받으면 호출부가 동 레이어를
 * 비운 채 넘어간다 — 틀린 숫자를 보여주는 것보다 안 보여주는 쪽이 낫다.
 *
 * 동/시군구/시도 오버레이([IosDongOverlayLoader])와 구간 인덱스 들여오기
 * ([IosRoadSegmentIndexImporter])가 같은 경로를 쓰므로 여기 모아 둔다.
 */
object IosServerDongData {

    /** 스플래시가 이미 쏴 둔 `/api/v1/meta`([MetaHolder])를 기다리는 최대 시간. */
    private const val META_WAIT_TIMEOUT_MS = 5_000L

    private val json = Json { ignoreUnknownKeys = true }

    /** 실패하거나([MetaHolder]가 아직 안 채워졌거나 오프라인) 하면 null. */
    suspend fun fetch(): ServerDongMapData? {
        val meta = withTimeoutOrNull(META_WAIT_TIMEOUT_MS) {
            MetaHolder.state.filterNotNull().first()
        } ?: return null
        return try {
            val version = meta.mapVersions.current
            val files = IosServerMapStore().ensureDownloaded(version, listOf("index.geojson", "index.json"))
            ServerDongMapData(
                boundaryGeoJson = IosDongOverlayBuilder.normalizeDongCodeKey(files.getValue("index.geojson")),
                totalLengthsByDong = json.decodeFromString<IndexJson>(files.getValue("index.json")).dongLengthM,
                mapVersion = version,
            )
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
private data class IndexJson(val dongLengthM: Map<String, Double> = emptyMap())
