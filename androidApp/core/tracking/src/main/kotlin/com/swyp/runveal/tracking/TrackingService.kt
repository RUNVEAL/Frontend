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
import com.swyp.runveal.core.tracking.R
import com.swyp.runveal.data.local.DatabaseDriverFactory
import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.data.local.TrackLocalStore
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.recording.RecordingState
import com.swyp.runveal.recording.TrackPoint
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.valhalla.ChunkedMatcher
import com.swyp.runveal.valhalla.defaultRoadMatcher
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val DONG_BOUNDARY_ASSET_PATH = "dong/korea_dong.geojson"
private const val MATCH_STATE_FAILED = "FAILED"

/**
 * 위치 수신 Foreground Service — `docs/running/recording-lifecycle.md` §3/§4.1/§5. 화면이 꺼져도
 * 기록이 끊기지 않게 알림을 띄워 둔다. 위치 권한은 이미 승인됐다고 전제한다(`core:permission`의
 * 흐름을 먼저 거친다) — 이 서비스는 권한을 직접 요청하지 않는다.
 *
 * GPS 요청 간격은 **5초**(문서 원안의 1초에서 조정 — 배터리/데이터량 고려). [TrackFilter]의 정확도/
 * 속도/거리 기준은 별개라 바뀌지 않는다.
 */
class TrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var fileStore: FileStore? = null
    private var trackLocalStore: TrackLocalStore? = null

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
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // 호출부(core:permission 흐름)가 먼저 보장한다.
    private fun startTracking() {
        if (TrackRecorder.state.value is RecordingState.Recording) return // 이미 기록 중이면 무시.

        TrackRecorder.start(id = UUID.randomUUID().toString(), startedAtEpochMs = System.currentTimeMillis())
        startForeground(NOTIFICATION_ID, buildNotification(elapsedText = "00:00", distanceText = "0.0km"))
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(5))
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5))
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
        store.saveTrack(
            id = finished.id,
            startedAtEpochMs = finished.startedAtEpochMs,
            finishedAtEpochMs = finished.finishedAtEpochMs,
            distanceMeters = TrackRecorder.totalDistanceMeters(),
            pointCountFiltered = finished.filteredPoints.size,
            pointCountRaw = finished.rawPoints.size,
            filteredGpx = buildGpx(finished.filteredPoints),
            rawGpx = buildGpx(finished.rawPoints),
            nowEpochMs = finished.finishedAtEpochMs,
        )
        serviceScope.launch { matchAndRecord(finished, store) }
    }

    /** §6 "최종본 — 전체 매칭과 저장". 점이 2000개를 넘으면 [ChunkedMatcher]가 알아서 쪼갠다. 실패해도
     * raw/filtered GPX는 이미 저장돼 있으니(§6-3) `match_state = FAILED`로만 남긴다 — 재시도 큐/수동
     * "다시 매칭" 버튼은 이번 범위 밖. */
    private suspend fun matchAndRecord(finished: RecordingState.Finished, store: TrackLocalStore) {
        val points = finished.filteredPoints.map { LatLon(lat = it.lat, lon = it.lon) }
        if (points.size < 2) return

        try {
            val result = ChunkedMatcher.matchInChunks(defaultRoadMatcher(), points)
            store.recordMatchResult(
                trackId = finished.id,
                edges = result.edges,
                confidenceScore = result.confidenceScore,
                unmatchedPointCount = result.unmatchedPointCount,
                boundaries = loadDongBoundaries(),
                nowEpochMs = System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            store.database.trackQueries.updateMatchState(
                match_state = MATCH_STATE_FAILED,
                confidence_score = null,
                unmatched_point_count = null,
                updated_at_epoch_ms = System.currentTimeMillis(),
                id = finished.id,
            )
        }
    }

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
