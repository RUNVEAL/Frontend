package com.sloth.runveal.splash

import android.content.Context
import android.util.Log
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.SegmentCoverage
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.dong.CoverageMask
import com.sloth.runveal.dong.MatchedEdgeParser
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

private const val TAG = "DebugTrackSeeder"

/**
 * 더미 경로로 쓸 **실제 매칭 기록** — `core:map`의 `assets/tracks`에 들어 있는, 지도 매칭까지 끝난
 * 러닝 기록이다. 그래서 좌표가 전부 도로 위에 있다(직접 만든 사각형 경로와 다르다).
 * 번들에 있는 것 중 가장 긴 파일(47점, 약 1.8km)을 골랐다.
 */
private const val SEED_TRACK_ASSET = "tracks/20261025_184613.matched.geojson"

/** 더미 기록이 "달린 것으로" 적립할 구간 수 — 많이 잡으면 지도가 온통 금색이 된다. */
private const val SEGMENTS_PER_TRACK = 12

/** 6'00"/km로 달린 셈 치고 시간을 만든다 — 거리에서 역산해 페이스가 그럴듯하게 나오게. */
private const val PACE_SEC_PER_KM = 360.0

/**
 * **개발용 더미 러닝 기록**. 기록 탭·기록 상세를 눈으로 확인하려면 기록이 있어야 하는데, 실제로
 * 달리지 않고는 만들 수가 없어서 둔다.
 *
 * 경로는 [SEED_TRACK_ASSET]의 실제 매칭 기록을 그대로 쓴다 — 도로를 따라가는 좌표라 썸네일·상세
 * 지도가 실제 기록과 똑같이 보인다. 시각만 "어제 저녁"으로 옮겨 붙인다.
 *
 * **이번 달 기록이 하나도 없을 때만** 한 번 넣는다 — 실제로 달린 기록이 있으면 아무것도 하지 않는다.
 * 지우는 건 기록 상세의 더보기 → 삭제로 하면 된다(그때 적립한 커버리지도 같이 걷힌다).
 *
 * ⚠️ **머지 전에 반드시 지울 것** — 이 파일과 [splashGraph]의 호출. 테스트 기기에 릴리즈 빌드만
 * 올리는 상황이라 디버그 빌드 가드를 뺐다. 그래서 지금 이 코드는 **릴리즈에서도 돌아간다**.
 */
internal object DebugTrackSeeder {

    fun seedIfEmpty(context: Context, zoneId: ZoneId = ZoneId.systemDefault()) {
        val store = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
        val now = System.currentTimeMillis()
        val monthStart = YearMonth.now(zoneId).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        if (store.tracksBetween(monthStart, now + 1).isNotEmpty()) return

        runCatching {
            val route = loadMatchedRoute(context)
            if (route.size < 2) {
                Log.w(TAG, "더미 경로가 비었다 — 에셋($SEED_TRACK_ASSET)을 확인할 것")
                return
            }
            seedOne(store, route, now)
            Log.i(TAG, "더미 기록 1건을 넣었다(${route.size}점) — 확인이 끝나면 이 코드를 지울 것")
        }.onFailure { Log.w(TAG, "더미 기록 넣기 실패 — 그냥 넘어간다", it) }
    }

    /** 매칭 기록은 "구간(edge) 목록"이라 이어 붙인다 — 앞 구간의 끝점과 다음 구간의 시작점이 같아서
     * 중복만 걸러내면 한 줄짜리 경로가 된다. */
    private fun loadMatchedRoute(context: Context): List<LatLon> {
        val json = context.assets.open(SEED_TRACK_ASSET).bufferedReader().use { it.readText() }
        return MatchedEdgeParser.parse(json)
            .flatMap { it.coordinates }
            .fold(mutableListOf<LatLon>()) { acc, point ->
                if (acc.lastOrNull() != point) acc.add(point)
                acc
            }
    }

    private fun seedOne(store: TrackLocalStore, route: List<LatLon>, nowEpochMs: Long) {
        val id = UUID.randomUUID().toString()
        val distanceMeters = GeoMath.totalDistanceMeters(route)
        val elapsedMs = (distanceMeters / 1000.0 * PACE_SEC_PER_KM * 1000).toLong()
        // 어제 저녁 7시쯤 달린 것으로 — 오늘 날짜에 붙이면 "방금 달린 기록"처럼 보여 헷갈린다.
        val startedAt = nowEpochMs - DAY_MILLIS
        val gpx = buildGpx(route, startedAt, elapsedMs)

        store.saveTrack(
            id = id,
            startedAtEpochMs = startedAt,
            finishedAtEpochMs = startedAt + elapsedMs,
            distanceMeters = distanceMeters,
            pointCountFiltered = route.size,
            pointCountRaw = route.size,
            filteredGpx = gpx,
            rawGpx = gpx,
            nowEpochMs = nowEpochMs,
        )

        // 경로를 `track_edge`에 넣어야 목록 썸네일·상세 지도가 그려진다. 적립할 구간은 경로 근처에서
        // 실제 인덱스에 있는 것만 고른다 — 없는 segment_id를 넣으면 조용히 버려지고(그래야 맞다),
        // 인덱스를 아직 안 들여왔으면 "새 길"과 탐험률 변화가 비어 있을 뿐이다.
        store.recordMatchResult(
            trackId = id,
            matchedPaths = listOf(route),
            coverages = nearbySegments(store, route),
            nowEpochMs = nowEpochMs,
        )
    }

    /** 경로 바운딩 박스와 겹치는 구간 몇 개를 "전부 달린 것"으로 본다. */
    private fun nearbySegments(store: TrackLocalStore, route: List<LatLon>): List<SegmentCoverage> {
        val lats = route.map { it.lat }
        val lons = route.map { it.lon }
        return store.database.roadSegmentIndexQueries
            .selectNear(
                maxLat = lats.max(),
                minLat = lats.min(),
                maxLon = lons.max(),
                minLon = lons.min(),
            )
            .executeAsList()
            .take(SEGMENTS_PER_TRACK)
            .map { segment ->
                // 실제 샘플 수는 recordMatchResult가 구간 지오메트리를 보고 다시 맞춘다 —
                // 여기선 "전부 덮었다"는 뜻만 담으면 된다.
                SegmentCoverage(segment.segment_id, CoverageMask.full(FULL_MASK_SIZE))
            }
    }

    /** `TrackingService`가 쓰는 것과 같은 최소 형식 — 점마다 좌표와 시각만 적는다. */
    private fun buildGpx(route: List<LatLon>, startedAtEpochMs: Long, elapsedMs: Long): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"RUNVEAL-DEBUG\">\n  <trk>\n    <trkseg>\n")
        route.forEachIndexed { index, point ->
            val at = startedAtEpochMs + elapsedMs * index / route.size.coerceAtLeast(1)
            append("      <trkpt lat=\"${point.lat}\" lon=\"${point.lon}\">")
            append("<time>${Instant.ofEpochMilli(at)}</time>")
            append("</trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }

    /** 커버리지 비트 수 — 실제 값은 구간마다 다시 맞춰지므로 아무 값이나 꽉 채우기만 하면 된다. */
    private const val FULL_MASK_SIZE = 8

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
}
