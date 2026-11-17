package com.swyp.runveal.ios

import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.dong.DongExploreRate
import com.swyp.runveal.network.meta.MetaHolder
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * 번들에 들어 있는 경계 에셋 원본 텍스트 — iOS 번들에서 읽는 건 Swift(`RunvealBundleAsset`) 몫이고,
 * 여기서는 넘겨받은 텍스트만 쓴다(Kotlin이 iOS 번들 구조를 알 필요가 없게).
 */
data class DongBundleAssets(
    val dongGeoJson: String,
    val sigunguGeoJson: String,
    val sidoGeoJson: String,
    val regionHierarchyJson: String,
    /** 탐험률 분모(동별 전체 도로 길이) 번들 추정치. 없으면 null — 그때는 전부 0%가 된다. */
    val dongLengthsJson: String?,
)

/**
 * 내 기록 기준 동/시군구/시도 오버레이를 만든다 — Android `core:map`의 `loadDongOverlayBundle`에 해당.
 *
 * 분자는 실제로 달려서 "가본 길"이 된 구간 길이(`explored_segment`), 분모는 그 동 전체 도로 길이다 —
 * 서버 `index.json`을 받았으면 그 값을, 못 받았으면 로컬에 들여온 구간 인덱스 집계를, 그것도 비었으면
 * 번들 추정치를 쓴다.
 */
object IosDongOverlayLoader {

    /** 스플래시가 이미 쏴 둔 `/api/v1/meta`([MetaHolder])를 기다리는 최대 시간 — 서버가 꺼져 있거나
     * 느려도 홈 지도는 번들 에셋으로 계속 떠야 한다(`docs/api/API.md` §5.7). */
    private const val META_WAIT_TIMEOUT_MS = 5_000L

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(assets: DongBundleAssets): DongOverlayGeoJsonBundle {
        val trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())

        val server = syncFromServer()
        val walkedLengths = trackLocalStore.walkedLengthByDong()
        val totalLengths = server?.totalLengthsByDong
            ?: trackLocalStore.totalLengthByDong().ifEmpty { bundleTotalLengths(assets.dongLengthsJson) }
        val dongPercents = DongExploreRate.percents(walkedLengths, totalLengths)

        return IosDongOverlayBuilder.build(
            dongBoundaryGeoJson = server?.boundaryGeoJson ?: assets.dongGeoJson,
            sigunguBoundaryGeoJson = assets.sigunguGeoJson,
            sidoBoundaryGeoJson = assets.sidoGeoJson,
            regionHierarchyJson = assets.regionHierarchyJson,
            dongPercents = dongPercents,
            boundarySourceKey = server?.let { "server:${it.mapVersion}" } ?: "asset",
        )
    }

    private data class ServerDongMapData(
        val boundaryGeoJson: String,
        val totalLengthsByDong: Map<String, Double>,
        val mapVersion: String,
    )

    /** 실패하거나([MetaHolder]가 아직 안 채워졌거나 오프라인) 하면 null — 호출부는 번들 에셋을 쓴다. */
    private suspend fun syncFromServer(): ServerDongMapData? {
        val meta = withTimeoutOrNull(META_WAIT_TIMEOUT_MS) {
            MetaHolder.state.filterNotNull().first()
        } ?: return null
        return try {
            val version = meta.mapVersions.current
            val files = IosServerMapStore().ensureDownloaded(version, listOf("index.geojson", "index.json"))
            val boundaryGeoJson = files.getValue("index.geojson").let(IosDongOverlayBuilder::normalizeDongCodeKey)
            val totalLengths = json.decodeFromString<IndexJson>(files.getValue("index.json")).dongLengthM
            ServerDongMapData(
                boundaryGeoJson = boundaryGeoJson,
                totalLengthsByDong = totalLengths,
                mapVersion = version,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun bundleTotalLengths(dongLengthsJson: String?): Map<String, Double> {
        if (dongLengthsJson == null) return emptyMap()
        return try {
            json.decodeFromString<Map<String, Double>>(dongLengthsJson)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

@kotlinx.serialization.Serializable
private data class IndexJson(val dongLengthM: Map<String, Double> = emptyMap())
