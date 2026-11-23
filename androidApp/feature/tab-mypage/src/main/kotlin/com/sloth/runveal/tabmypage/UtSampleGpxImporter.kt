package com.sloth.runveal.tabmypage

import android.content.Context
import android.util.Log
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.data.local.TrackSource
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import java.time.Instant
import java.util.UUID

private const val TAG = "UtSampleGpx"

/** 번들에 넣어 둔 UT 샘플 GPX 폴더(`src/main/assets/ut-gpx`). */
private const val ASSET_DIR = "ut-gpx"

/** `track.external_id` 접두사 — 같은 파일을 두 번 넣지 않게 DB의 UNIQUE 제약으로 막는다. */
private const val EXTERNAL_ID_PREFIX = "ut-sample:"

/** 시각이 없는 GPX를 만났을 때 쓸 페이스(6'00"/km) — 거리에서 소요 시간을 역산한다. */
private const val FALLBACK_PACE_SEC_PER_KM = 360.0

/** `<trkpt lat=".." lon="..">…</trkpt>` 한 점. UT 샘플이 쓰는 형식만 다룬다(자체 종료 태그 없음). */
private val TRKPT_REGEX = Regex("""<trkpt\s+lat="([-\d.]+)"\s+lon="([-\d.]+)"[^>]*>(.*?)</trkpt>""", RegexOption.DOT_MATCHES_ALL)
private val TIME_REGEX = Regex("""<time>([^<]+)</time>""")

internal data class UtSampleGpxImportResult(val imported: Int, val skipped: Int)

/**
 * **테스트용 샘플 기록 넣기**(마이페이지 → "테스트 GPX 불러오기"). UT에서 기록 탭·기록 상세·동
 * 탐험률을 실제 데이터로 보려면 기록이 있어야 하는데, 직접 달리지 않고는 만들 수가 없어서 둔다.
 *
 * 번들 GPX(수원 영통 일대, 3~7km 5건)를 **GPX에 적힌 시각 그대로** 저장한다 — 날짜를 오늘로 옮기지
 * 않아서 기록 탭의 월별 묶임도 같이 확인할 수 있다.
 *
 * 이미 넣은 파일은 `external_id`로 걸러 건너뛴다. 지우는 건 기록 상세의 더보기 → 삭제로 하면 되고,
 * 지운 뒤 다시 누르면 그 건만 다시 들어온다.
 *
 * ⚠️ **머지 전에 지울 것** — 이 파일, `assets/ut-gpx`, 그리고 [MyScreen]의 메뉴 줄.
 */
internal object UtSampleGpxImporter {

    fun import(context: Context): UtSampleGpxImportResult {
        val store = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
        val alreadyImported = store.importedExternalIds(TrackSource.LOCAL)
        val fileNames = context.assets.list(ASSET_DIR)?.filter { it.endsWith(".gpx") }?.sorted().orEmpty()
        if (fileNames.isEmpty()) Log.w(TAG, "에셋($ASSET_DIR)에 GPX가 없다")

        var imported = 0
        var skipped = 0
        fileNames.forEach { fileName ->
            val externalId = EXTERNAL_ID_PREFIX + fileName
            if (externalId in alreadyImported) {
                skipped++
                return@forEach
            }
            val ok = runCatching { importOne(context, store, fileName, externalId) }
                .onFailure { Log.w(TAG, "$fileName 넣기 실패 — 건너뛴다", it) }
                .getOrDefault(false)
            if (ok) imported++ else skipped++
        }
        Log.i(TAG, "샘플 기록 ${imported}건 추가, ${skipped}건 건너뜀")
        return UtSampleGpxImportResult(imported = imported, skipped = skipped)
    }

    private fun importOne(context: Context, store: TrackLocalStore, fileName: String, externalId: String): Boolean {
        val gpx = context.assets.open("$ASSET_DIR/$fileName").bufferedReader().use { it.readText() }
        val points = parse(gpx)
        if (points.size < 2) {
            Log.w(TAG, "$fileName — 좌표가 ${points.size}개뿐이라 건너뛴다")
            return false
        }

        val path = points.map { it.location }
        val distanceMeters = GeoMath.totalDistanceMeters(path)
        val startedAt = points.firstNotNullOfOrNull { it.timeEpochMs }
            ?: (System.currentTimeMillis() - elapsedMsFor(distanceMeters))
        val finishedAt = points.lastOrNull { it.timeEpochMs != null }?.timeEpochMs
            ?: (startedAt + elapsedMsFor(distanceMeters))
        val now = System.currentTimeMillis()
        val trackId = UUID.randomUUID().toString()

        store.saveTrack(
            id = trackId,
            startedAtEpochMs = startedAt,
            finishedAtEpochMs = finishedAt,
            distanceMeters = distanceMeters,
            pointCountFiltered = path.size,
            pointCountRaw = path.size,
            // 받은 GPX를 그대로 저장한다 — 앱이 쓰는 형식과 같은 1Hz trkpt 목록이다.
            filteredGpx = gpx,
            rawGpx = gpx,
            nowEpochMs = now,
            source = TrackSource.LOCAL,
            externalId = externalId,
        )

        // 실제 러닝 종료와 같은 처리를 한다(TrackingService): 경로를 `track_edge`에 넣어 목록 썸네일·
        // 상세 지도가 그려지게 하고, 로컬 `road_segment` 인덱스에 올라탄 부분만 탐험률로 적립한다.
        // 그 지역 인덱스를 아직 안 들여왔으면 적립만 비고(경로는 그대로 보인다) 조용히 넘어간다.
        val summary = store.recordMatchResult(
            trackId = trackId,
            matchedPaths = listOf(path),
            coverages = store.coverageOnPaths(listOf(path)),
            nowEpochMs = now,
        )
        Log.i(
            TAG,
            "$fileName — ${path.size}점 %.2fkm, 새 길 %.0fm, 동 ${summary.touchedDongAdmCodes.size}개"
                .format(distanceMeters / 1000.0, summary.newRoadMeters),
        )
        return true
    }

    /** `<trkpt>` 목록을 좌표 + 시각으로 읽는다. 시작·종료 시각만 쓰므로 중간 점의 시각이 없어도 된다. */
    private fun parse(gpx: String): List<GpxPoint> = TRKPT_REGEX.findAll(gpx).mapNotNull { match ->
        val lat = match.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
        val timeEpochMs = TIME_REGEX.find(match.groupValues[3])
            ?.groupValues?.get(1)
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        GpxPoint(LatLon(lat = lat, lon = lon), timeEpochMs)
    }.toList()

    private fun elapsedMsFor(distanceMeters: Double): Long =
        (distanceMeters / 1000.0 * FALLBACK_PACE_SEC_PER_KM * 1000).toLong()

    private data class GpxPoint(val location: LatLon, val timeEpochMs: Long?)
}
