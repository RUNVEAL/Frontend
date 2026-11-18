package com.sloth.runveal.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.core.content.ContextCompat
import com.sloth.runveal.core.map.DongAssetRepository
import com.sloth.runveal.core.map.LiveRoadClassifier
import com.sloth.runveal.core.map.RoadSegmentIndexImporter
import com.sloth.runveal.core.tracking.R
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.match.ChunkedMatcher
import com.sloth.runveal.match.MatchedPathFilter
import com.sloth.runveal.match.ServerRoadMatcher
import com.sloth.runveal.network.match.MatchPurpose
import com.sloth.runveal.network.meta.MetaApi
import com.sloth.runveal.network.meta.MetaHolder
import com.sloth.runveal.recording.RecordingState
import com.sloth.runveal.recording.TrackPoint
import com.sloth.runveal.recording.TrackRecorder
import com.sloth.runveal.result.DongRateChange
import com.sloth.runveal.result.DongRateChanges
import com.sloth.runveal.result.LiveMatchHolder
import com.sloth.runveal.result.LiveNewRoadHolder
import com.sloth.runveal.result.TrackResult
import com.sloth.runveal.result.TrackResultHolder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "TrackingService"
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
        trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, fileStore!!)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent가 null이면 OS가 START_STICKY로 되살린 것이다(프로세스가 죽은 뒤). 그때 기록을
        // 다시 시작하면 안 된다 — 사용자가 시작하지도 않은 러닝이 부활하고, 죽은 원인이 그대로면
        // 되살아나자마자 또 죽어 앱을 켤 때마다 꺼지는 상태가 된다.
        if (intent == null) {
            android.util.Log.w(TAG, "시스템이 되살린 서비스 — 기록을 다시 시작하지 않고 끝낸다")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_STOP -> stopTracking(saveRecord = intent.getBooleanExtra(EXTRA_SAVE_RECORD, true))
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

    @SuppressLint("MissingPermission") // 바로 아래에서 직접 확인한다.
    private fun startTracking() {
        if (TrackRecorder.state.value is RecordingState.Recording) return // 이미 기록 중이면 무시.

        // 호출부(HomeScreen.withLocationReady)가 먼저 받아 두지만, 여기서 한 번 더 본다 — 권한
        // 없이 들어오면 requestLocationUpdates()가 SecurityException을 던지고 그대로 앱이 죽는다.
        // 기록을 못 시작하는 건 참을 수 있어도 죽는 건 안 된다. 권한이 러닝 도중 회수되는 경우도
        // 있어서(설정에서 끄기) 호출부 보장만 믿을 수 없다.
        if (!hasLocationPermission()) {
            android.util.Log.w(TAG, "위치 권한이 없어 기록을 시작하지 않는다")
            stopSelf()
            return
        }

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
                    // 서버는 도로 데이터가 없는 구간을 도로망을 따라 메워서 돌려준다 — 그대로 그리면
                    // 가지도 않은 길이 "오늘 달린 길"로 보인다. 실제 GPS와 떨어진 부분은 잘라낸다.
                    LiveMatchHolder.publish(MatchedPathFilter.keepNearRecordedPoints(result.path, points))
                } catch (e: Exception) {
                    android.util.Log.w("ServerMatchDebug", "실시간 매칭 실패 points=${points.size}", e)
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(5))
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5))
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    /** [saveRecord]가 false면 100m/1분을 못 채운 러닝이라 GPX·track row를 아예 만들지 않고 버린다
     * (판단은 호출부 — `RecordingViewModel`이 [com.sloth.runveal.recording.RecordingSavePolicy]로 한다).
     * 그때는 [TrackResultHolder]도 publish하지 않는다 — 결과 화면으로 넘어가지 않으니 기다리는 쪽이 없다. */
    private fun stopTracking(saveRecord: Boolean) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        liveMatchJob?.cancel()
        liveMatchJob = null
        LiveMatchHolder.clear()
        LiveNewRoadHolder.clear()
        val finished = TrackRecorder.finish(finishedAtEpochMs = System.currentTimeMillis())
        if (finished != null && saveRecord) {
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
            publishResult(finished, distanceMeters, elapsedMs, newRoadMeters = 0.0, dongStats = emptyList(), routePaths = listOf(points))
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
            val beforeWalked = store.walkedLengthByDong()
            // 서버 매칭 결과를 두 군데서 거른다 — 안 거르면 안 지난 길이 "가본 길"로 그려지고
            // 동 탐험률도 부풀려진다(서버가 도로 데이터 없는 구간을 도로망으로 메워서 주기 때문).
            //  - 경로: 실제 GPS와 동떨어진 부분을 잘라내고 조각으로 나눈다.
            //  - 구간: 로컬 구간 인덱스의 도로선과 대조해 GPS가 진짜 지나간 것만 적립한다.
            val matchedPaths = MatchedPathFilter.keepNearRecordedPoints(result.path, points)
            val runPaths = matchedPaths.ifEmpty { listOf(points) }
            // 탐험률에 적립할 구간도 러닝 중 색 판정과 **같은 규칙**으로 고른다 — 서버 segmentIds를
            // 그대로 믿으면 안 지난 도로가 섞여 들어오고, 반대로 로컬 인덱스에 없는 ID만 와서
            // 아무것도 적립 안 되는 일도 생긴다(러닝 중엔 새 길이었는데 탐험률은 그대로인 원인).
            // 구간 전체가 아니라 그 구간에서 **지나간 부분만** 적립한다(TrackLocalStore.coverageOnPaths).
            val coverages = store.coverageOnPaths(runPaths)

            // "새로 탐험한 길"은 러닝 중 배지·금색 선과 **같은 기준**으로 센다 — 달린 선에서 아직 안
            // 가본 도로 위를 지난 부분의 길이다. 적립(구간 단위)으로 세면 러닝 중엔 0.38km였는데 결과는
            // 0km로 나오는 일이 생긴다: 그 구간이 로컬 인덱스에 없거나 이미 적립돼 있으면 0이 되기 때문.
            // 적립보다 **먼저** 분류해야 이번 러닝으로 막 적립될 구간이 "가본 길"로 바뀌기 전 상태를 본다.
            val classifier = LiveRoadClassifier(store.database).also { it.refreshExploredSegments() }
            val classified = runPaths.map { classifier.classify(it) }
            val newRoadMeters = classified.sumOf { it.newRoadMeters }
            val newRoadPaths = classified.flatMap { it.newRuns }

            val summary = store.recordMatchResult(
                trackId = finished.id,
                matchedPaths = matchedPaths,
                coverages = coverages,
                nowEpochMs = System.currentTimeMillis(),
            )
            val afterWalked = store.walkedLengthByDong()
            val dongStats = buildDongStats(summary.touchedDongAdmCodes, beforeWalked, afterWalked)
            publishResult(
                finished, distanceMeters, elapsedMs, newRoadMeters, dongStats,
                // 결과 카드도 기록 탭 썸네일과 같은 선을 그리도록 **매칭 경로**를 넘긴다. 원본 GPS를
                // 그리면 도로를 들쭉날쭉 벗어나서, 도로를 따라 그려지는 금색 "새로 탐험한 길"과 어긋나
                // 보인다 — 같은 러닝인데 기록 탭에선 도로에 붙고 결과 화면에선 안 붙는 이유였다.
                // 매칭이 점 2개도 못 돌려주면 원본 GPS로 떨어진다.
                // 지도의 가본 길·러닝 중 선과 **같은** 경로를 쓴다 — 서버가 도로 데이터 없는 구간을
                // 메워 준 부분을 결과 카드만 그대로 그리면, 러닝 중 보던 선과 전혀 다른 길이 나온다.
                routePaths = runPaths,
                // 금색 덧그림도 구간 전체가 아니라 **실제로 지난 부분**이다 — 러닝 중 금색 선과 같다.
                newRoadPaths = newRoadPaths,
            )
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
            publishResult(finished, distanceMeters, elapsedMs, newRoadMeters = 0.0, dongStats = emptyList(), routePaths = listOf(points))
        }
    }

    private fun publishResult(
        finished: RecordingState.Finished,
        distanceMeters: Double,
        elapsedMs: Long,
        newRoadMeters: Double,
        dongStats: List<DongRateChange>,
        routePaths: List<List<LatLon>>,
        newRoadPaths: List<List<LatLon>> = emptyList(),
    ) {
        TrackResultHolder.publish(
            TrackResult(
                trackId = finished.id,
                startedAtEpochMs = finished.startedAtEpochMs,
                distanceMeters = distanceMeters,
                elapsedMs = elapsedMs,
                newRoadMeters = newRoadMeters,
                dongStats = dongStats,
                routePaths = routePaths,
                newRoadPaths = newRoadPaths,
            ),
        )
    }

    /** 이번 트랙이 닿은 동만 — 거르는 규칙과 계산은 [DongRateChanges]에 있다(기록 불러오기 결과와
     * 같은 숫자를 보여 줘야 해서 공용). */
    private fun buildDongStats(
        touchedAdmCodes: Set<String>,
        beforeWalked: Map<String, Double>,
        afterWalked: Map<String, Double>,
    ): List<DongRateChange> {
        if (touchedAdmCodes.isEmpty()) return emptyList()
        // 동 이름은 계층 파일(150KB)에서 꺼낸다 — 예전엔 이름 하나 때문에 4.3MB 경계 geoJSON을
        // 통째로 파싱했다. 계층 파일도 서버 코드 체계로 만들어서 `adm_cd2`가 그대로 맞는다.
        val hierarchy = DongAssetRepository(this).loadRegionHierarchy()
        return DongRateChanges.build(
            touchedAdmCodes = touchedAdmCodes,
            beforeWalked = beforeWalked,
            afterWalked = afterWalked,
            totalLengths = trackLocalStore?.totalLengthByDong() ?: emptyMap(),
            namesByCode = hierarchy.sido
                .flatMap { sido -> sido.sigungu.flatMap { it.dongs } }
                .associate { it.code to it.name },
        )
    }

    /** 앱 실행 시 스플래시가 받아 둔 버전([MetaHolder])이 있으면 그대로 쓰고, 없으면(스플래시 호출
     * 실패·아직 전) 여기서 한 번 더 받는다 — `/match` 요청엔 `mapVersion`이 꼭 있어야 한다. */
    private suspend fun resolveMapVersion(): String =
        MetaHolder.state.value?.mapVersions?.current
            ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }.mapVersions.current

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
        private const val ACTION_STOP = "com.sloth.runveal.tracking.ACTION_STOP"
        private const val ACTION_PAUSE = "com.sloth.runveal.tracking.ACTION_PAUSE"
        private const val ACTION_RESUME = "com.sloth.runveal.tracking.ACTION_RESUME"
        private const val EXTRA_SAVE_RECORD = "com.sloth.runveal.tracking.EXTRA_SAVE_RECORD"

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

        /** [saveRecord]가 false면 이번 러닝을 기록으로 남기지 않고 버린다(너무 짧은 러닝). */
        fun stop(context: Context, saveRecord: Boolean = true) {
            val intent = Intent(context, TrackingService::class.java)
                .setAction(ACTION_STOP)
                .putExtra(EXTRA_SAVE_RECORD, saveRecord)
            context.startService(intent)
        }
    }
}
