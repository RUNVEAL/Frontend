package com.sloth.runveal.splash

import android.util.Log
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.SegmentCoverage
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.dong.CoverageMask
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.util.UUID

private const val TAG = "DebugTrackSeeder"

/** 더미 기록 개수. 목록 스크롤과 썸네일이 섞여 보이게 몇 건 넣는다. */
private const val SEED_COUNT = 5

/** 더미 경로 한 변의 점 개수 — 네 변을 돌아 사각형 한 바퀴가 된다. */
private const val POINTS_PER_SIDE = 12

/** 더미 기록 하나가 "달린 것으로" 적립할 구간 수 — 많이 잡으면 지도가 온통 금색이 된다. */
private const val SEGMENTS_PER_TRACK = 12

/** 기록마다 경로를 조금씩 옮겨 겹치지 않게 하는 간격(위도). 약 200m. */
private const val SEED_OFFSET_DEGREES = 0.0018

/** 성수동 근처 — 더미 경로의 기준점. */
private val SEED_CENTER = LatLon(lat = 37.5445, lon = 127.0557)

/**
 * **개발용 더미 러닝 기록**. 기록 탭·기록 상세를 눈으로 확인하려면 기록이 있어야 하는데, 실제로
 * 달리지 않고는 만들 수가 없어서 둔다.
 *
 * **이번 달 기록이 하나도 없을 때만** 한 번 넣는다 — 실제로 달린 기록이 있으면 아무것도 하지 않는다.
 * 지우는 건 기록 상세의 더보기 → 삭제로 하면 된다(그때 적립한 커버리지도 같이 걷힌다).
 *
 * ⚠️ **머지 전에 반드시 지울 것** — 이 파일과 [splashGraph]의 호출. 테스트 기기에 릴리즈 빌드만
 * 올리는 상황이라 디버그 빌드 가드를 뺐다. 그래서 지금 이 코드는 **릴리즈에서도 돌아간다** —
 * 이대로 배포하면 실제 사용자 기기에 더미 기록 5건이 들어간다.
 */
internal object DebugTrackSeeder {

    fun seedIfEmpty(zoneId: ZoneId = ZoneId.systemDefault()) {
        val store = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())
        val now = System.currentTimeMillis()
        val monthStart = YearMonth.now(zoneId).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        if (store.tracksBetween(monthStart, now + 1).isNotEmpty()) return

        runCatching {
            repeat(SEED_COUNT) { index -> seedOne(store, index, now) }
            Log.i(TAG, "더미 기록 ${SEED_COUNT}건을 넣었다 — 머지 전에 이 코드를 지울 것")
        }.onFailure { Log.w(TAG, "더미 기록 넣기 실패 — 그냥 넘어간다", it) }
    }

    private fun seedOne(store: TrackLocalStore, index: Int, nowEpochMs: Long) {
        val id = UUID.randomUUID().toString()
        // 하루 간격으로 뒤로 거슬러 올라가며, 길이도 조금씩 다르게 — 목록이 한 덩어리로 안 보이게.
        val startedAt = nowEpochMs - (index + 1) * DAY_MILLIS - index * HOUR_MILLIS
        val elapsedMs = (25 + index * 7) * MINUTE_MILLIS
        val route = squareRoute(
            center = LatLon(
                lat = SEED_CENTER.lat + index * SEED_OFFSET_DEGREES,
                lon = SEED_CENTER.lon + index * SEED_OFFSET_DEGREES,
            ),
            sideDegrees = 0.004 + index * 0.001,
        )
        val gpx = buildGpx(route, startedAt, elapsedMs)

        store.saveTrack(
            id = id,
            startedAtEpochMs = startedAt,
            finishedAtEpochMs = startedAt + elapsedMs,
            distanceMeters = GeoMath.totalDistanceMeters(route),
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

    /** 사각형 한 바퀴 — 실제 길을 따라가지는 않지만 썸네일·지도에 경로가 보이기엔 충분하다. */
    private fun squareRoute(center: LatLon, sideDegrees: Double): List<LatLon> {
        val half = sideDegrees / 2
        val corners = listOf(
            LatLon(center.lat - half, center.lon - half),
            LatLon(center.lat + half, center.lon - half),
            LatLon(center.lat + half, center.lon + half),
            LatLon(center.lat - half, center.lon + half),
            LatLon(center.lat - half, center.lon - half),
        )
        return corners.zipWithNext().flatMap { (from, to) ->
            (0 until POINTS_PER_SIDE).map { step ->
                val ratio = step.toDouble() / POINTS_PER_SIDE
                LatLon(
                    lat = from.lat + (to.lat - from.lat) * ratio,
                    lon = from.lon + (to.lon - from.lon) * ratio,
                )
            }
        } + corners.last()
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

    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS
}
