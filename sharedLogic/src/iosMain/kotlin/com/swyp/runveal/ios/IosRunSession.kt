package com.swyp.runveal.ios

import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.DongExploreRate
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.match.ChunkedMatcher
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.recording.TrackPoint
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.result.DongRateChange
import com.swyp.runveal.result.LiveMatchHolder
import com.swyp.runveal.result.LiveNewRoadHolder
import com.swyp.runveal.result.TrackResult
import com.swyp.runveal.result.TrackResultHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSUUID
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

/** 러닝 중 도로 매칭 재요청 간격 — `/meta`의 분당 30회 제한(§5.6)에 여유 있게 걸리고,
 * §3.6.1의 "10초 간격으로 줄인 것" 전처리 단위와도 맞춘다. */
private const val LIVE_MATCH_INTERVAL_MS = 10_000L

private const val MATCH_STATE_FAILED = "FAILED"

/**
 * 러닝 한 번의 수명을 관리한다 — Android `TrackingService`의 **로직 부분**과 같다
 * (`docs/running/recording-lifecycle.md` §3/§4.1/§5/§6).
 *
 * 위치 수신(CoreLocation)과 백그라운드 유지는 플랫폼 몫이라 Swift가 하고
 * ([RunvealRunTracker]), 여기로는 [addLocation]으로 점만 넣어 준다. 그 외 — 실시간 매칭 루프,
 * 종료 시 GPX·DB 저장, 최종 매칭, 구간 인덱스 들여오기, 결과 publish — 는 모두 여기서 끝낸다.
 *
 * Android는 Foreground Service 알림을 띄우지만 iOS는 알림 대신 백그라운드 위치 모드를 쓴다 —
 * 알림 관련 코드가 없는 게 그 차이다.
 */
class IosRunSession(
    /** 번들 동 경계 원본 — 결과 화면의 동별 탐험률 변화에 쓸 동 이름을 찾는다. */
    private val dongBoundaryGeoJson: String,
    /** 번들 계층 원본 — 달린 구의 구간 인덱스를 들여올 때 쓴다. */
    private val regionHierarchyJson: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fileStore = FileStore()
    private val trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, fileStore)
    private var liveMatchJob: Job? = null

    /** 이미 기록 중이면 무시한다. */
    fun start(nowEpochMs: Long) {
        if (RunvealRecorder.snapshot().phase == RecordingPhase.RECORDING) return

        // 이전 결과가 남아 있으면 새 결과 화면이 잠깐 옛 값을 보여준다.
        TrackResultHolder.clear()
        // 지난 러닝의 매칭 선이 새 러닝 시작 직후 잠깐 겹쳐 보이지 않게.
        LiveMatchHolder.clear()
        // 같은 이유 — 새 러닝은 "새로 탐험한 길 0.00km"부터 시작해야 한다.
        LiveNewRoadHolder.clear()

        TrackRecorder.start(id = NSUUID().UUIDString, startedAtEpochMs = nowEpochMs)
        startLiveMatchLoop()
    }

    /** 위치 수신을 멈추는 건 Swift 쪽 — 여기서는 기록 상태만 바꾼다. */
    fun pause(nowEpochMs: Long) {
        TrackRecorder.pause(nowEpochMs)
    }

    fun resume(nowEpochMs: Long) {
        TrackRecorder.resume(nowEpochMs)
    }

    /**
     * [saveRecord]가 false면 100m/1분을 못 채운 러닝이라 GPX·track row를 아예 만들지 않고 버린다
     * (판단은 호출부 — `RecordingSavePolicy`). 그때는 [TrackResultHolder]도 publish하지 않는다 —
     * 결과 화면으로 넘어가지 않으니 기다리는 쪽이 없다.
     */
    fun stop(nowEpochMs: Long, saveRecord: Boolean) {
        liveMatchJob?.cancel()
        liveMatchJob = null
        LiveMatchHolder.clear()
        LiveNewRoadHolder.clear()

        val finished = RunvealRecorder.finish(finishedAtEpochMs = nowEpochMs)
        if (finished != null && saveRecord) {
            scope.launch { saveFinishedTrack(finished) }
        }
        TrackRecorder.reset()
    }

    /** CoreLocation이 받은 점 하나 — 녹화 중이 아니면 조용히 무시된다. */
    fun addLocation(
        lat: Double,
        lon: Double,
        accuracyMeters: Float,
        speedMps: Float,
        timeEpochMs: Long,
    ) {
        TrackRecorder.addRawPoint(
            TrackPoint(
                lat = lat,
                lon = lon,
                accuracyMeters = accuracyMeters,
                speedMps = speedMps,
                timeEpochMs = timeEpochMs,
            ),
        )
    }

    fun dispose() {
        liveMatchJob?.cancel()
        liveMatchJob = null
    }

    /**
     * 녹화 중 주기적으로 지금까지의 필터링된 점을 그대로 `/match`(LIVE)에 보내 도로에 맞춘 선을
     * [LiveMatchHolder]에 publish한다. 일시정지 중엔 새 점이 없어 건너뛴다. 실패해도 큐/재시도 없이
     * 다음 주기에 다시 시도 — 화면은 마지막 성공 결과를 그대로 보여준다.
     */
    private fun startLiveMatchLoop() {
        liveMatchJob?.cancel()
        liveMatchJob = scope.launch {
            while (true) {
                delay(LIVE_MATCH_INTERVAL_MS)
                if (RunvealRecorder.snapshot().phase != RecordingPhase.RECORDING) continue
                val points = RunvealRecorder.currentFilteredPath()
                if (points.size < 2) continue
                try {
                    val matcher = IosServerRoadMatcher(resolveMapVersion(), MatchPurpose.LIVE)
                    LiveMatchHolder.publish(ChunkedMatcher.matchInChunks(matcher, points).path)
                } catch (_: Exception) {
                    // 다음 주기에 다시 시도한다.
                }
            }
        }
    }

    /** §5 "기록 종료 시": GPX 먼저 쓰고 성공해야 `track` insert. 저장 후 §6 최종본 매칭을 이어서 돌린다. */
    private suspend fun saveFinishedTrack(finished: RecordingSnapshot) {
        val filteredPoints = finished.filteredPoints
        val distanceMeters = GeoMath.totalDistanceMeters(
            filteredPoints.map { LatLon(lat = it.lat, lon = it.lon) },
        )
        val id = finished.id ?: return
        trackLocalStore.saveTrack(
            id = id,
            startedAtEpochMs = finished.startedAtEpochMs,
            finishedAtEpochMs = finished.finishedAtEpochMs,
            distanceMeters = distanceMeters,
            pointCountFiltered = filteredPoints.size,
            // Android는 raw 점도 따로 저장하는데, iOS는 `RecordingSnapshot`에 filtered만 담아
            // 넘기므로 같은 목록을 쓴다 — raw GPX는 필터 디버깅용이라 기능에 영향이 없다.
            pointCountRaw = filteredPoints.size,
            filteredGpx = buildGpx(filteredPoints),
            rawGpx = buildGpx(filteredPoints),
            nowEpochMs = finished.finishedAtEpochMs,
        )
        matchAndRecord(finished, id, distanceMeters)
    }

    /**
     * §6 "최종본 — 전체 매칭과 저장". 점이 2000개를 넘으면 `ChunkedMatcher`가 알아서 쪼갠다. 실패해도
     * GPX는 이미 저장돼 있으니 `match_state = FAILED`로만 남긴다. 성공/실패/점 부족 어느 경우든 결과
     * 화면이 무한 로딩에 빠지지 않게 [TrackResultHolder]는 항상 publish한다.
     */
    private suspend fun matchAndRecord(finished: RecordingSnapshot, trackId: String, distanceMeters: Double) {
        val points = finished.filteredPoints.map { LatLon(lat = it.lat, lon = it.lon) }
        val elapsedMs = finished.finishedAtEpochMs - finished.startedAtEpochMs

        if (points.size < 2) {
            publishResult(trackId, finished, distanceMeters, elapsedMs, 0.0, emptyList(), points, emptyList())
            return
        }

        try {
            val matcher = IosServerRoadMatcher(resolveMapVersion(), MatchPurpose.LIVE)
            val result = ChunkedMatcher.matchInChunks(matcher, points)

            // 이번에 달린 구의 road_segment 인덱스가 없으면 여기서 들여온다 — 없으면 segmentId를
            // 길이/동으로 못 바꿔서 이번 결과가 전부 집계에서 빠진다.
            IosRoadSegmentIndexImporter().importIfNeeded(
                sigunguCodes = result.sigunguCodes,
                bundleDongGeoJson = dongBoundaryGeoJson,
                regionHierarchyJson = regionHierarchyJson,
            )

            val boundaries = DongBoundaryParser.parse(dongBoundaryGeoJson)
            val beforeWalked = trackLocalStore.walkedLengthByDong()
            val summary = trackLocalStore.recordMatchResult(
                trackId = trackId,
                matchedPath = result.path,
                segmentIds = result.segmentIds,
                nowEpochMs = nowEpochMillis(),
            )
            val afterWalked = trackLocalStore.walkedLengthByDong()

            publishResult(
                trackId = trackId,
                finished = finished,
                distanceMeters = distanceMeters,
                elapsedMs = elapsedMs,
                newRoadMeters = summary.newRoadMeters,
                dongStats = buildDongStats(summary.touchedDongAdmCodes, beforeWalked, afterWalked, boundaries),
                // 결과 카드도 기록 탭 썸네일과 같은 선을 그리도록 **매칭 경로**를 넘긴다. 매칭이 점
                // 2개도 못 돌려주면 원본 GPS로 떨어진다.
                routePoints = result.path.takeIf { it.size >= 2 } ?: points,
                newRoadPaths = summary.newRoadPaths,
            )
        } catch (_: Exception) {
            trackLocalStore.database.trackQueries.updateMatchState(
                match_state = MATCH_STATE_FAILED,
                confidence_score = null,
                unmatched_point_count = null,
                updated_at_epoch_ms = nowEpochMillis(),
                id = trackId,
            )
            publishResult(trackId, finished, distanceMeters, elapsedMs, 0.0, emptyList(), points, emptyList())
        }
    }

    private fun publishResult(
        trackId: String,
        finished: RecordingSnapshot,
        distanceMeters: Double,
        elapsedMs: Long,
        newRoadMeters: Double,
        dongStats: List<DongRateChange>,
        routePoints: List<LatLon>,
        newRoadPaths: List<List<LatLon>>,
    ) {
        TrackResultHolder.publish(
            TrackResult(
                trackId = trackId,
                startedAtEpochMs = finished.startedAtEpochMs,
                distanceMeters = distanceMeters,
                elapsedMs = elapsedMs,
                newRoadMeters = newRoadMeters,
                dongStats = dongStats,
                routePoints = routePoints,
                newRoadPaths = newRoadPaths,
            ),
        )
    }

    /** 이번 트랙이 닿은 동만 — 전체 길이가 없거나 이름을 못 찾으면 그 동은 뺀다. */
    private fun buildDongStats(
        touchedAdmCodes: Set<String>,
        beforeWalked: Map<String, Double>,
        afterWalked: Map<String, Double>,
        boundaries: List<DongBoundary>,
    ): List<DongRateChange> {
        if (touchedAdmCodes.isEmpty()) return emptyList()
        val totalLengths = trackLocalStore.totalLengthByDong()
        val namesByCode = boundaries.associate { it.admCd2 to it.name }
        return touchedAdmCodes.mapNotNull { admCd2 ->
            val total = totalLengths[admCd2]?.takeIf { it > 0.0 } ?: return@mapNotNull null
            val name = namesByCode[admCd2] ?: return@mapNotNull null
            DongRateChange(
                admCd2 = admCd2,
                name = name,
                beforePercent = DongExploreRate.percentOf(beforeWalked[admCd2] ?: 0.0, total),
                afterPercent = DongExploreRate.percentOf(afterWalked[admCd2] ?: 0.0, total),
            )
        }
    }

    /** 앱 실행 시 스플래시가 받아 둔 버전([MetaHolder])이 있으면 그대로 쓰고, 없으면 여기서 한 번 더
     * 받는다 — `/match` 요청엔 `mapVersion`이 꼭 있어야 한다. */
    private suspend fun resolveMapVersion(): String =
        MetaHolder.state.value?.mapVersions?.current
            ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current

    private fun buildGpx(points: List<TrackPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"RUNVEAL\">\n  <trk>\n    <trkseg>\n")
        val formatter = NSISO8601DateFormatter()
        points.forEach { point ->
            val date = NSDate.dateWithTimeIntervalSince1970(point.timeEpochMs / 1000.0)
            append("      <trkpt lat=\"${point.lat}\" lon=\"${point.lon}\">")
            append("<time>${formatter.stringFromDate(date)}</time>")
            append("<speed>${point.speedMps}</speed>")
            append("</trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }

    private fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}
