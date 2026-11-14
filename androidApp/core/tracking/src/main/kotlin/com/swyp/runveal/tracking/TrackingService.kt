package com.swyp.runveal.tracking

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.swyp.runveal.core.map.RoadSegmentIndexImporter
import com.swyp.runveal.core.tracking.R
import com.swyp.runveal.data.local.DatabaseDriverFactory
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.match.ChunkedMatcher
import com.swyp.runveal.network.match.MatchPurpose
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.recording.RecordingState
import com.swyp.runveal.recording.TrackPoint
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.result.DongRateChange
import com.swyp.runveal.result.LiveMatchHolder
import com.swyp.runveal.result.LiveNewRoadHolder
import com.swyp.runveal.result.TrackResult
import com.swyp.runveal.result.TrackResultHolder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DONG_BOUNDARY_ASSET_PATH = "dong/korea_dong.geojson"
private const val MATCH_STATE_FAILED = "FAILED"

/** 러닝 중 도로 매칭 재요청 간격 — `/meta`의 분당 30회 제한(§5.6)에 여유 있게 걸리고, §3.6.1의
 * "10초 간격으로 줄인 것" 전처리 단위와도 맞춘다. */
private val LIVE_MATCH_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10)

/**
 * 위치 수신 Foreground Service — `docs/running/recording-lifecycle.md` §3/§4.1/§5. 화면이 꺼져도
 * 기록이 끊기지 않게 알림을 띄워 둔다. 위치 권한은 이미 승인됐다고 전제한다(`core:permission`의
 * 흐름을 먼저 거친다) — 이 서비스는 권한을 직접 요청하지 않는다.
 *
 * GPS 요청 간격은 **5초**(문서 원안의 1초에서 조정 — 배터리/데이터량 고려). [TrackFilter]의 정확도/
 * 속도/거리 기준은 별개라 바뀌지 않는다.
 *
 * 녹화 중엔 [LIVE_MATCH_INTERVAL_MS](10초)마다 그때까지의 점을 통째로 `/match`(LIVE)에 다시 보내
 * [LiveMatchHolder]를 갈아 끼운다 — 러닝 종료 후 최종 매칭([matchAndRecord])과 같은 호출을 여러 번
 * 반복하는 것뿐, 별도 "실시간" 전용 엔드포인트는 없다.
 */
class TrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var fileStore: FileStore? = null
    private var trackLocalStore: TrackLocalStore? = null
    private var liveMatchJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val point = TrackPoint(
                lat = location.latitude,
                lon = location.longitude,
                accuracyMeters = location.accuracy,
                speedMps = if (location.hasSpeed()) location.speed else 0f,
                timeEpochMs = location.time,
            )
            TrackRecorder.addRawPoint(point)
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fileStore = FileStore()
        trackLocalStore = TrackLocalStore(RunvealDatabase(DatabaseDriverFactory().createDriver()), fileStore!!)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            else -> startTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        liveMatchJob?.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // 호출부(core:permission 흐름)가 먼저 보장한다.
    private fun startTracking() {
        if (TrackRecorder.state.value is RecordingState.Recording) return // 이미 기록 중이면 무시.

        TrackResultHolder.clear() // 이전 결과가 남아 있으면 새 결과 화면이 잠깐 옛 값을 보여준다.
        LiveMatchHolder.clear() // 지난 러닝의 매칭 선이 새 러닝 시작 직후 잠깐 겹쳐 보이지 않게.
        LiveNewRoadHolder.clear() // 같은 이유 — 새 러닝은 "새로 탐험한 길 0.00km"부터 시작해야 한다.
        TrackRecorder.start(id = UUID.randomUUID().toString(), startedAtEpochMs = System.currentTimeMillis())
        startForeground(NOTIFICATION_ID, buildNotification(elapsedText = "00:00", distanceText = "0.0km"))
        requestLocationUpdates()
        startLiveMatchLoop()
    }

    /** 녹화 중 주기적으로 지금까지의 필터링된 점을 그대로 `/match`(LIVE)에 보내 도로에 맞춘 선을
     * [LiveMatchHolder]에 publish한다 — §3.6과 같은 호출이지만 러닝이 끝나기 전에 여러 번 부른다.
     * 일시정지 중엔 새 점이 없어 다시 보낼 이유가 없어 건너뛴다. 실패해도 큐/재시도 없이 다음 주기에
     * 다시 시도 — 화면은 마지막 성공 결과를 그대로 보여준다. */
    private fun startLiveMatchLoop() {
        liveMatchJob?.cancel()
        liveMatchJob = serviceScope.launch {
            while (true) {
                delay(LIVE_MATCH_INTERVAL_MS)
                if (TrackRecorder.state.value !is RecordingState.Recording) continue
                val points = TrackRecorder.currentFilteredPoints().map { LatLon(lat = it.lat, lon = it.lon) }
                if (points.size < 2) continue
                try {
                    val mapVersion = resolveMapVersion()
                    val matcher = ServerRoadMatcher(mapVersion = mapVersion, purpose = MatchPurpose.LIVE)
                    val result = ChunkedMatcher.matchInChunks(matcher, points)
                    LiveMatchHolder.publish(result.path)
                } catch (e: Exception) {
                    android.util.Log.w("ServerMatchDebug", "실시간 매칭 실패 points=${points.size}", e)
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(5))
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5))
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        liveMatchJob?.cancel()
        liveMatchJob = null
        LiveMatchHolder.clear()
        LiveNewRoadHolder.clear()
        val finished = TrackRecorder.finish(finishedAtEpochMs = System.currentTimeMillis())
        if (finished != null) {
            serviceScope.launch { saveFinishedTrack(finished) }
        }
        TrackRecorder.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** 위치 수신만 멈춘다 — Foreground Service 자체는 유지(알림도 그대로, 다만 더는 갱신되지 않는다). */
    private fun pauseTracking() {
        if (TrackRecorder.state.value !is RecordingState.Recording) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        TrackRecorder.pause(nowEpochMs = System.currentTimeMillis())
    }

    @SuppressLint("MissingPermission") // startTracking()과 동일한 전제.
    private fun resumeTracking() {
        if (TrackRecorder.state.value !is RecordingState.Paused) return
        TrackRecorder.resume(nowEpochMs = System.currentTimeMillis())
        requestLocationUpdates()
    }

    /** §5 "기록 종료 시": GPX 먼저 쓰고 성공해야 `track` insert — [TrackLocalStore.saveTrack]이 그 순서를
     * 그대로 지킨다. 저장 후 §6 최종본 매칭을 이어서 돌린다. */
    private fun saveFinishedTrack(finished: RecordingState.Finished) {
        val store = trackLocalStore ?: return
        val distanceMeters = GeoMath.totalDistanceMeters(finished.filteredPoints.map { LatLon(lat = it.lat, lon = it.lon) })
        store.saveTrack(
            id = finished.id,
            startedAtEpochMs = finished.startedAtEpochMs,
            finishedAtEpochMs = finished.finishedAtEpochMs,
            distanceMeters = distanceMeters,
            pointCountFiltered = finished.filteredPoints.size,
            pointCountRaw = finished.rawPoints.size,
            filteredGpx = buildGpx(finished.filteredPoints),
            rawGpx = buildGpx(finished.rawPoints),
            nowEpochMs = finished.finishedAtEpochMs,
        )
        serviceScope.launch { matchAndRecord(finished, store, distanceMeters) }
    }

    /** §6 "최종본 — 전체 매칭과 저장". 점이 2000개를 넘으면 [ChunkedMatcher]가 알아서 쪼갠다. 실패해도
     * raw/filtered GPX는 이미 저장돼 있으니(§6-3) `match_state = FAILED`로만 남긴다 — 재시도 큐/수동
     * "다시 매칭" 버튼은 이번 범위 밖. 성공/실패/점 부족 어느 경우든 결과 화면이 무한 로딩에 빠지지
     * 않게 [TrackResultHolder]는 항상 publish한다. */
    private suspend fun matchAndRecord(finished: RecordingState.Finished, store: TrackLocalStore, distanceMeters: Double) {
        val points = finished.filteredPoints.map { LatLon(lat = it.lat, lon = it.lon) }
        val elapsedMs = finished.finishedAtEpochMs - finished.startedAtEpochMs

        if (points.size < 2) {
            publishResult(finished, distanceMeters, elapsedMs, newRoadMeters = 0.0, dongStats = emptyList(), routePoints = points)
            return
        }

        try {
            val mapVersion = resolveMapVersion()
            val matcher = ServerRoadMatcher(mapVersion = mapVersion, purpose = MatchPurpose.LIVE)
            val result = ChunkedMatcher.matchInChunks(matcher, points)
            // TEMP DEBUG — 실서버 /match 교체가 실제로 되는지 확인한다. 확인 끝나면 지운다.
            android.util.Log.i(
                "ServerMatchDebug",
                "매칭 성공 points=${points.size} segmentIds=${result.segmentIds.size} pathPoints=${result.path.size} mapVersion=$mapVersion",
            )
            // 이번에 달린 구의 road_segment 인덱스가 없으면 여기서 들여온다 — 없으면 segmentId를
            // 길이/동으로 못 바꿔서 이번 결과가 전부 집계에서 빠진다. 전국을 들여오면 896MB·400만
            // 구간이라 끝나지 않으므로 달린 구만 받는다(§3.6.2 sigunguCodes).
            RoadSegmentIndexImporter(this, store.database).importIfNeeded(result.sigunguCodes)
            val boundaries = loadDongBoundaries()
            val beforeWalked = store.walkedLengthByDong()
            val summary = store.recordMatchResult(
                trackId = finished.id,
                matchedPath = result.path,
                segmentIds = result.segmentIds,
                nowEpochMs = System.currentTimeMillis(),
            )
            val afterWalked = store.walkedLengthByDong()
            val dongStats = buildDongStats(summary.touchedDongAdmCodes, beforeWalked, afterWalked, boundaries)
            publishResult(finished, distanceMeters, elapsedMs, summary.newRoadMeters, dongStats, routePoints = points)
        } catch (e: Exception) {
            // TEMP DEBUG — 확인 끝나면 catch (_: Exception)으로 되돌린다.
            android.util.Log.w("ServerMatchDebug", "매칭 실패 points=${points.size}", e)
            store.database.trackQueries.updateMatchState(
                match_state = MATCH_STATE_FAILED,
                confidence_score = null,
                unmatched_point_count = null,
                updated_at_epoch_ms = System.currentTimeMillis(),
                id = finished.id,
            )
            publishResult(finished, distanceMeters, elapsedMs, newRoadMeters = 0.0, dongStats = emptyList(), routePoints = points)
        }
    }

    private fun publishResult(
        finished: RecordingState.Finished,
        distanceMeters: Double,
        elapsedMs: Long,
        newRoadMeters: Double,
        dongStats: List<DongRateChange>,
        routePoints: List<LatLon>,
    ) {
        TrackResultHolder.publish(
            TrackResult(
                trackId = finished.id,
                startedAtEpochMs = finished.startedAtEpochMs,
                distanceMeters = distanceMeters,
                elapsedMs = elapsedMs,
                newRoadMeters = newRoadMeters,
                dongStats = dongStats,
                routePoints = routePoints,
            ),
        )
    }

    /** 이번 트랙이 닿은 동만 — 전체 길이가 없거나(road_segment 인덱스에 없는 동) 이름을 못 찾으면 그
     * 동은 뺀다. */
    private fun buildDongStats(
        touchedAdmCodes: Set<String>,
        beforeWalked: Map<String, Double>,
        afterWalked: Map<String, Double>,
        boundaries: List<DongBoundary>,
    ): List<DongRateChange> {
        if (touchedAdmCodes.isEmpty()) return emptyList()
        val totalLengths = trackLocalStore?.totalLengthByDong() ?: emptyMap()
        val namesByCode = boundaries.associate { it.admCd2 to it.name }
        return touchedAdmCodes.mapNotNull { admCd2 ->
            val total = totalLengths[admCd2]?.takeIf { it > 0.0 } ?: return@mapNotNull null
            val name = namesByCode[admCd2] ?: return@mapNotNull null
            DongRateChange(
                admCd2 = admCd2,
                name = name,
                beforePercent = percentOf(beforeWalked[admCd2] ?: 0.0, total),
                afterPercent = percentOf(afterWalked[admCd2] ?: 0.0, total),
            )
        }
    }

    private fun percentOf(walked: Double, total: Double): Int = ((walked / total) * 100).toInt().coerceIn(0, 100)

    /** 앱 실행 시 스플래시가 받아 둔 버전([MetaHolder])이 있으면 그대로 쓰고, 없으면(스플래시 호출
     * 실패·아직 전) 여기서 한 번 더 받는다 — `/match` 요청엔 `mapVersion`이 꼭 있어야 한다. */
    private suspend fun resolveMapVersion(): String =
        MetaHolder.state.value?.mapVersions?.current
            ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current

    private fun loadDongBoundaries(): List<DongBoundary> {
        val geoJson = assets.open(DONG_BOUNDARY_ASSET_PATH).bufferedReader().use { it.readText() }
        return DongBoundaryParser.parse(geoJson)
    }

    private fun buildGpx(points: List<TrackPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"RUNVEAL\">\n  <trk>\n    <trkseg>\n")
        points.forEach { point ->
            val instant = java.time.Instant.ofEpochMilli(point.timeEpochMs)
            append("      <trkpt lat=\"${point.lat}\" lon=\"${point.lon}\">")
            append("<time>$instant</time>")
            append("<speed>${point.speedMps}</speed>")
            append("</trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.tracking_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(elapsedText: String, distanceText: String): android.app.Notification {
        val stopIntent = Intent(this, TrackingService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            0,
            stopIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text_format, elapsedText, distanceText))
            .setSmallIcon(R.drawable.ic_notification_tracking)
            .setOngoing(true)
            .addAction(0, "종료", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        if (TrackRecorder.state.value !is RecordingState.Recording) return
        val elapsedSeconds = TrackRecorder.elapsedMs(System.currentTimeMillis()) / 1000
        val elapsedText = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
        val distanceKm = TrackRecorder.totalDistanceMeters() / 1000.0
        val distanceText = "%.1fkm".format(distanceKm)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(elapsedText, distanceText))
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.swyp.runveal.tracking.ACTION_STOP"
        private const val ACTION_PAUSE = "com.swyp.runveal.tracking.ACTION_PAUSE"
        private const val ACTION_RESUME = "com.swyp.runveal.tracking.ACTION_RESUME"

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_PAUSE)
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_RESUME)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
