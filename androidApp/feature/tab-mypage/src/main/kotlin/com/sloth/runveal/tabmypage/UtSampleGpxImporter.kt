package com.sloth.runveal.tabmypage

import android.content.Context
import android.util.Log
import com.sloth.runveal.core.map.RoadSegmentIndexImporter
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.data.local.TrackSource
import com.sloth.runveal.external.ExternalWorkoutPoint
import com.sloth.runveal.external.MatchPointReducer
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.match.ChunkedMatcher
import com.sloth.runveal.match.MatchedPathFilter
import com.sloth.runveal.match.ServerRoadMatcher
import com.sloth.runveal.network.match.MatchPurpose
import com.sloth.runveal.network.meta.MetaApi
import com.sloth.runveal.network.meta.MetaHolder
import com.sloth.runveal.result.TrackResult
import com.sloth.runveal.result.TrackResultHolder
import java.time.Instant
import java.util.UUID

private const val TAG = "UtSampleGpx"

/** 번들에 넣어 둔 UT 샘플 GPX 폴더(`src/main/assets/ut-gpx`). */
private const val ASSET_DIR = "ut-gpx"

/** `track.external_id` 접두사 — 같은 파일을 두 번 넣지 않게 DB의 UNIQUE 제약으로 막는다. */
private const val EXTERNAL_ID_PREFIX = "ut-sample:"

/** 시각이 없는 GPX를 만났을 때 쓸 페이스(6'00"/km) — 거리에서 소요 시간을 역산한다. */
private const val FALLBACK_PACE_SEC_PER_KM = 360.0

/**
 * 샘플 경로가 있는 구(경기 수원시 영통구) — 서버 매칭이 실패했을 때 쓰는 구간 인덱스 들여오기 대상.
 *
 * 평소엔 `/match` 응답의 `sigunguCodes`를 쓴다(러닝 종료·기록 가져오기와 같은 흐름). 서버가 죽어
 * 있으면 그 값을 못 받는데, 그러면 인덱스가 없어서 탐험률·러닝 중 흰색 판정이 통째로 비게 되니
 * **샘플 데이터가 있는 구를 알고 있다는 점**을 이용해 직접 지정한다.
 */
private const val SAMPLE_SIGUNGU_CODE = "41117"

/** `<trkpt lat=".." lon="..">…</trkpt>` 한 점. UT 샘플이 쓰는 형식만 다룬다(자체 종료 태그 없음). */
private val TRKPT_REGEX = Regex("""<trkpt\s+lat="([-\d.]+)"\s+lon="([-\d.]+)"[^>]*>(.*?)</trkpt>""", RegexOption.DOT_MATCHES_ALL)
private val TIME_REGEX = Regex("""<time>([^<]+)</time>""")

internal data class UtSampleGpxImportResult(val imported: Int, val skipped: Int)

/**
 * **테스트용 샘플 기록 넣기**(마이페이지 → "테스트 GPX 불러오기"). UT에서 기록 탭·기록 상세·동
 * 탐험률을 실제 데이터로 보려면 기록이 있어야 하는데, 직접 달리지 않고는 만들 수가 없어서 둔다.
 *
 * 번들 GPX(경기 수원시 영통구 일대, 3~7km 5건)를 **GPX에 적힌 시각 그대로** 저장한다 — 날짜를
 * 오늘로 옮기지 않아서 기록 탭의 월별 묶임도 같이 확인할 수 있다.
 *
 * 저장 뒤 처리는 **기록 가져오기(`HealthConnectImporter.matchAndRecord`)와 같은 순서**다 — 서버
 * 매칭 → 그 구의 구간 인덱스 들여오기 → 지나간 부분만 적립. 그래야 넣은 기록이 실제로 달린 기록과
 * 똑같이 취급된다: 홈 지도의 "가본 길"과 동 탐험률, 러닝 중 흰색/금색 판정([LiveRoadClassifier]),
 * 러닝 결과의 "새로 탐험한 길"까지 전부 이 적립을 근거로 본다. 적립을 건너뛰면 선만 보이고 나머지는
 * 전부 0이 된다.
 *
 * 이미 넣은 파일은 `external_id`로 걸러 건너뛴다. 지우는 건 기록 상세의 더보기 → 삭제로 하면 되고,
 * 지운 뒤 다시 누르면 그 건만 다시 들어온다.
 *
 * ⚠️ **머지 전에 지울 것** — 이 파일, `assets/ut-gpx`, 그리고 [MyScreen]의 메뉴 줄.
 */
internal object UtSampleGpxImporter {

    suspend fun import(context: Context): UtSampleGpxImportResult {
        val store = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
        val alreadyImported = store.importedExternalIds(TrackSource.LOCAL)
        val fileNames = context.assets.list(ASSET_DIR)?.filter { it.endsWith(".gpx") }?.sorted().orEmpty()
        if (fileNames.isEmpty()) Log.w(TAG, "에셋($ASSET_DIR)에 GPX가 없다")

        var imported = 0
        var skipped = 0
        var lastResult: TrackResult? = null
        fileNames.forEach { fileName ->
            val externalId = EXTERNAL_ID_PREFIX + fileName
            if (externalId in alreadyImported) {
                skipped++
                return@forEach
            }
            val result = runCatching { importOne(context, store, fileName, externalId) }
                .onFailure { Log.w(TAG, "$fileName 넣기 실패 — 건너뛴다", it) }
                .getOrNull()
            if (result != null) {
                imported++
                lastResult = result
            } else {
                skipped++
            }
        }
        // 홈 지도는 [TrackResultHolder]가 바뀔 때만 "가본 길"과 동 탐험률을 DB에서 다시 읽는다
        // (HomeScreen 주석) — 러닝 종료와 같은 신호를 보내 줘야 앱을 다시 켜지 않고도 반영된다.
        lastResult?.let(TrackResultHolder::publish)
        Log.i(TAG, "샘플 기록 ${imported}건 추가, ${skipped}건 건너뜀")
        return UtSampleGpxImportResult(imported = imported, skipped = skipped)
    }

    /** 넣었으면 그 트랙의 [TrackResult], 못 넣었으면 null. */
    private suspend fun importOne(
        context: Context,
        store: TrackLocalStore,
        fileName: String,
        externalId: String,
    ): TrackResult? {
        val gpx = context.assets.open("$ASSET_DIR/$fileName").bufferedReader().use { it.readText() }
        val points = parse(gpx)
        if (points.size < 2) {
            Log.w(TAG, "$fileName — 좌표가 ${points.size}개뿐이라 건너뛴다")
            return null
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

        // 서버 매칭 → 구간 인덱스 → 적립. 거르는 규칙까지 러닝 종료·기록 가져오기와 같아야 같은
        // 길인데 출처에 따라 탐험률이 달라지는 일이 없다.
        val runPaths = matchOnRoads(context, store, fileName, points)
        val summary = store.recordMatchResult(
            trackId = trackId,
            matchedPaths = runPaths,
            coverages = store.coverageOnPaths(runPaths),
            nowEpochMs = now,
        )
        Log.i(
            TAG,
            "$fileName — ${path.size}점 %.2fkm, 새 길 %.0fm, 동 ${summary.touchedDongAdmCodes.size}개"
                .format(distanceMeters / 1000.0, summary.newRoadMeters),
        )

        return TrackResult(
            trackId = trackId,
            startedAtEpochMs = startedAt,
            distanceMeters = distanceMeters,
            elapsedMs = finishedAt - startedAt,
            newRoadMeters = summary.newRoadMeters,
            // 결과 화면용 값이라 비워 둔다 — 홈 지도는 trackId가 바뀌었는지만 본다.
            dongStats = emptyList(),
            routePaths = runPaths,
            newRoadPaths = summary.newRoadPaths,
        )
    }

    /**
     * 달린 선을 도로에 맞춘다(`docs/api/API.md` §3.6) — 그 구의 `road_segment` 인덱스가 없으면
     * 여기서 받아 온다. 인덱스가 없으면 segmentId를 길이·행정동으로 바꿀 수 없어서 적립이 통째로
     * 비고, 그러면 러닝 중 판정과 결과 화면도 이 기록을 모르는 상태가 된다.
     *
     * 서버 매칭이 실패하면(오프라인·503) 원본 GPS 선을 그대로 쓴다 — 선은 보이고, 인덱스만 있으면
     * 적립도 된다(샘플 경로의 구는 [SAMPLE_SIGUNGU_CODE]로 알고 있다).
     */
    private suspend fun matchOnRoads(
        context: Context,
        store: TrackLocalStore,
        fileName: String,
        points: List<GpxPoint>,
    ): List<List<LatLon>> {
        val path = points.map { it.location }
        val indexImporter = RoadSegmentIndexImporter(context, store.database)
        // 1Hz 원본은 서버 상한(2,000점)을 넘기도 해서 기록 가져오기와 같은 축약을 거친다(10초 간격
        // 샘플링 + 양끝 제자리 구간 잘라내기). 시각이 없는 점은 1Hz로 가정해 채운다.
        val reduced = MatchPointReducer.reduce(
            points.mapIndexed { index, point ->
                ExternalWorkoutPoint(
                    lat = point.location.lat,
                    lon = point.location.lon,
                    timeEpochMs = point.timeEpochMs ?: (index * 1000L),
                )
            },
        ).map { it.toLatLon() }.ifEmpty { path }

        val result = runCatching {
            val matcher = ServerRoadMatcher(mapVersion = resolveMapVersion(), purpose = MatchPurpose.IMPORT)
            ChunkedMatcher.matchInChunks(matcher, reduced)
        }.onFailure { Log.w(TAG, "$fileName — 서버 매칭 실패, 원본 GPS 선으로 적립한다", it) }.getOrNull()

        val sigunguCodes = result?.sigunguCodes?.takeIf { it.isNotEmpty() } ?: listOf(SAMPLE_SIGUNGU_CODE)
        val indexReady = indexImporter.importIfNeeded(sigunguCodes)
        if (!indexReady) Log.w(TAG, "$fileName — 구간 인덱스($sigunguCodes)를 못 들여왔다. 탐험률은 비게 된다")

        val matchedPaths = result?.path?.let { MatchedPathFilter.keepNearRecordedPoints(it, reduced) }.orEmpty()
        return matchedPaths.ifEmpty { listOf(path) }
    }

    private suspend fun resolveMapVersion(): String =
        MetaHolder.state.value?.mapVersions?.current
            ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current

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
