package com.sloth.runveal.ios

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
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

/** `track.external_id` 접두사 — 같은 파일을 두 번 넣지 않게 DB의 UNIQUE 제약으로 막는다. */
private const val EXTERNAL_ID_PREFIX = "ut-sample:"

/** 시각이 없는 GPX를 만났을 때 쓸 페이스(km당 360초) — 거리에서 소요 시간을 역산한다. */
private const val FALLBACK_PACE_SEC_PER_KM = 360.0

/**
 * 샘플 경로가 있는 구(경기 수원시 영통구) — 서버 매칭이 실패했을 때 쓰는 구간 인덱스 들여오기 대상.
 *
 * 평소엔 `/match` 응답의 `sigunguCodes`를 쓴다(러닝 종료와 같은 흐름). 서버가 죽어 있으면 그 값을
 * 못 받는데, 그러면 인덱스가 없어서 탐험률·러닝 중 흰색 판정이 통째로 비게 되니 **샘플 데이터가
 * 있는 구를 알고 있다는 점**을 이용해 직접 지정한다.
 */
private const val SAMPLE_SIGUNGU_CODE = "41117"

/** `<trkpt lat=".." lon="..">…</trkpt>` 한 점. UT 샘플이 쓰는 형식만 다룬다(자체 종료 태그 없음). */
private val TRKPT_REGEX =
    Regex("<trkpt\\s+lat=\"([-\\d.]+)\"\\s+lon=\"([-\\d.]+)\"[^>]*>(.*?)</trkpt>", RegexOption.DOT_MATCHES_ALL)
private val TIME_REGEX = Regex("<time>([^<]+)</time>")

/** 넣은 건수와 건너뛴 건수. 화면은 이걸 그대로 안내 문구로 보여준다. */
data class UtSampleGpxImportResult(val imported: Int, val skipped: Int)

/**
 * **테스트용 샘플 기록 넣기**(마이페이지 → "UT 샘플 GPX 불러오기"). Android
 * `UtSampleGpxImporter`를 iOS로 옮긴 것으로, 흐름과 상수를 그대로 맞췄다.
 *
 * UT에서 기록 탭·기록 상세·동 탐험률을 실제 데이터로 보려면 기록이 있어야 하는데, 직접 달리지
 * 않고는 만들 수가 없어서 둔다. 번들 GPX(경기 수원시 영통구 일대, 3~7km 5건)를 **GPX에 적힌 시각
 * 그대로** 저장해서 기록 탭의 월별 묶임도 같이 확인할 수 있다.
 *
 * 저장 뒤 처리는 **러닝 종료(`IosRunSession`의 최종 매칭)와 같은 순서**다 — 서버 매칭 → 그 구의
 * 구간 인덱스 들여오기 → 지나간 부분만 적립. 그래야 넣은 기록이 실제로 달린 기록과 똑같이
 * 취급된다: 홈 지도의 "가본 길"과 동 탐험률, 러닝 중 흰색/금색 판정, 러닝 결과의 "새로 탐험한
 * 길"까지 전부 이 적립을 근거로 본다. 적립을 건너뛰면 선만 보이고 나머지는 전부 0이 된다.
 *
 * 이미 넣은 파일은 `external_id`로 걸러 건너뛴다. 지우는 건 기록 상세의 더보기 → 삭제로 하면 되고,
 * 지운 뒤 다시 누르면 그 건만 다시 들어온다.
 *
 * ⚠️ **머지 전에 지울 것** — 이 파일, 번들의 `ut-gpx` 에셋과 복사 태스크, 마이페이지의 메뉴 줄.
 */
@OptIn(ExperimentalForeignApi::class)
object IosUtSampleGpx {

    /**
     * 소수점 초가 있는 형식과 없는 형식을 모두 받는다 — 샘플 GPX마다 다를 수 있어서 둘 다 둔다
     * (`IosGpxPoints`와 같은 이유·같은 구성).
     */
    private val timeFormatters: List<NSISO8601DateFormatter> by lazy {
        listOf(
            NSISO8601DateFormatter().apply {
                setFormatOptions(
                    NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds,
                )
            },
            NSISO8601DateFormatter().apply {
                setFormatOptions(NSISO8601DateFormatWithInternetDateTime)
            },
        )
    }

    /**
     * 번들에 든 샘플 GPX를 전부 넣는다.
     *
     * 이름이 `import`가 아닌 이유: Swift 예약어라 호출할 때마다 백틱이 필요해진다.
     *
     * @param gpxByFileName 파일 이름 → GPX 본문. 번들에서 읽는 일은 Swift가 맡는다
     *  (`RunvealBundleAsset`이 번들 경로를 알고 있어서, Kotlin에서 다시 뒤지면 그 로직이 갈린다).
     * @param regionHierarchyJson 구간 인덱스를 들여올 때 쓰는 시도→시군구→동 계층
     *  (러닝 세션이 받는 것과 같은 값).
     */
    suspend fun importSamples(
        gpxByFileName: Map<String, String>,
        regionHierarchyJson: String,
    ): UtSampleGpxImportResult {
        val store = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
        val alreadyImported = store.importedExternalIds(TrackSource.LOCAL)

        var imported = 0
        var skipped = 0
        var lastResult: TrackResult? = null

        gpxByFileName.keys.sorted().forEach { fileName ->
            val externalId = EXTERNAL_ID_PREFIX + fileName
            if (externalId in alreadyImported) {
                skipped++
                return@forEach
            }
            val gpx = gpxByFileName[fileName]
            val result = if (gpx == null) {
                null
            } else {
                try {
                    importOne(store, gpx, externalId, regionHierarchyJson)
                } catch (_: Exception) {
                    null
                }
            }
            if (result != null) {
                imported++
                lastResult = result
            } else {
                skipped++
            }
        }

        // 홈 지도는 [TrackResultHolder]가 바뀔 때만 "가본 길"과 동 탐험률을 DB에서 다시 읽는다 —
        // 러닝 종료와 같은 신호를 보내 줘야 앱을 다시 켜지 않고도 반영된다.
        lastResult?.let { TrackResultHolder.publish(it) }
        return UtSampleGpxImportResult(imported = imported, skipped = skipped)
    }

    /** 넣었으면 그 트랙의 [TrackResult], 못 넣었으면 null. */
    private suspend fun importOne(
        store: TrackLocalStore,
        gpx: String,
        externalId: String,
        regionHierarchyJson: String,
    ): TrackResult? {
        val points = parse(gpx)
        if (points.size < 2) return null

        val path = points.map { it.location }
        val distanceMeters = GeoMath.totalDistanceMeters(path)
        val startedAt = points.firstNotNullOfOrNull { it.timeEpochMs }
            ?: (nowEpochMillis() - elapsedMsFor(distanceMeters))
        val finishedAt = points.lastOrNull { it.timeEpochMs != null }?.timeEpochMs
            ?: (startedAt + elapsedMsFor(distanceMeters))
        val now = nowEpochMillis()
        val trackId = NSUUID().UUIDString

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

        val runPaths = matchOnRoads(points, regionHierarchyJson)
        val summary = store.recordMatchResult(
            trackId = trackId,
            matchedPaths = runPaths,
            coverages = store.coverageOnPaths(runPaths),
            nowEpochMs = now,
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
     * 달린 선을 도로에 맞춘다 — 그 구의 `road_segment` 인덱스가 없으면 여기서 받아 온다. 인덱스가
     * 없으면 segmentId를 길이·행정동으로 바꿀 수 없어서 적립이 통째로 빈다.
     *
     * 서버 매칭이 실패하면(오프라인·503) 원본 GPS 선을 그대로 쓴다 — 선은 보이고, 인덱스만 있으면
     * 적립도 된다(샘플 경로의 구는 [SAMPLE_SIGUNGU_CODE]로 알고 있다).
     */
    private suspend fun matchOnRoads(
        points: List<GpxPoint>,
        regionHierarchyJson: String,
    ): List<List<LatLon>> {
        val path = points.map { it.location }
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
        ).map { LatLon(lat = it.lat, lon = it.lon) }.ifEmpty { path }

        val result = try {
            val matcher = ServerRoadMatcher(resolveMapVersion(), MatchPurpose.IMPORT)
            ChunkedMatcher.matchInChunks(matcher, reduced)
        } catch (_: Exception) {
            null
        }

        val sigunguCodes = result?.sigunguCodes?.takeIf { it.isNotEmpty() } ?: listOf(SAMPLE_SIGUNGU_CODE)
        IosRoadSegmentIndexImporter().importIfNeeded(
            sigunguCodes = sigunguCodes,
            regionHierarchyJson = regionHierarchyJson,
        )

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
            ?.let { parseEpochMs(it) }
        GpxPoint(LatLon(lat = lat, lon = lon), timeEpochMs)
    }.toList()

    /** Android는 `java.time.Instant`를 쓰지만 그건 Android 전용이라 Foundation 쪽을 쓴다. */
    private fun parseEpochMs(text: String): Long? {
        timeFormatters.forEach { formatter ->
            formatter.dateFromString(text)?.let { return (it.timeIntervalSince1970 * 1000).toLong() }
        }
        return null
    }

    private fun elapsedMsFor(distanceMeters: Double): Long =
        (distanceMeters / 1000.0 * FALLBACK_PACE_SEC_PER_KM * 1000).toLong()

    private fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    private data class GpxPoint(val location: LatLon, val timeEpochMs: Long?)
}
