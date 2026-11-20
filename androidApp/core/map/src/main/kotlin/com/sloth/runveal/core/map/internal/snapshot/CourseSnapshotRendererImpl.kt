package com.sloth.runveal.core.map.internal.snapshot

import android.content.Context
import android.graphics.Bitmap
import com.sloth.runveal.core.map.CourseSnapshotRenderer
import com.sloth.runveal.map.snapshot.SnapshotConfig
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.zoomToFit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter

private const val SNAPSHOT_TIMEOUT_MS = 15_000L

/**
 * [CourseSnapshotRenderer] 구현. [routeColor]/[endpointColor]/[endpointStrokeColor]는 Compose 컨텍스트가
 * 없는 이 레이어에 Composable 쪽([CourseSnapshotImage])이 [MapColors]와 같은 패턴으로 미리 변환해
 * 넘긴다. 시작·끝 점은 (이전 `ResultScreen.RouteMarker`와 달리) 한 레이어에서 같은 스타일로 그려서
 * 색을 하나만 받는다 — 문서(§2)는 "디자인 토큰 사용"만 요구할 뿐 두 색을 요구하지 않는다.
 */
internal class CourseSnapshotRendererImpl(
    private val context: Context,
    private val routeColor: Int,
    private val startColor: Int,
    private val startStrokeColor: Int,
    private val endpointColor: Int,
    private val endpointStrokeColor: Int,
    private val newRoadColor: Int,
    private val todayRoadColor: Int,
) : CourseSnapshotRenderer {
    private val density = context.resources.displayMetrics.density

    /** GL 컨텍스트 하나당 스냅샷 1개 — 동시 렌더링을 전역 1개로 제한한다. */
    private val renderMutex = Mutex()
    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 같은 캐시 키에 요청이 겹치면 하나만 렌더링하고 나머지는 그 `Deferred`를 같이 기다린다. */
    private val inFlight = mutableMapOf<String, Deferred<File?>>()
    private val inFlightLock = Mutex()

    override suspend fun getOrRender(request: SnapshotRequest, widthPx: Int, heightPx: Int): File? {
        cachedFile(request, widthPx, heightPx)?.let { return it }

        val key = fileKey(request, widthPx, heightPx)
        val deferred = inFlightLock.withLock {
            inFlight.getOrPut(key) {
                renderScope.async {
                    try {
                        renderMutex.withLock { renderAndCache(request, widthPx, heightPx) }
                    } finally {
                        inFlightLock.withLock { inFlight.remove(key) }
                    }
                }
            }
        }
        return deferred.await()
    }

    override fun cachedFile(request: SnapshotRequest, widthPx: Int, heightPx: Int): File? =
        CourseSnapshotDiskCache.file(context, request, widthPx, heightPx, density).takeIf { it.exists() }

    override fun delete(ownerId: String) {
        CourseSnapshotDiskCache.deleteAllForOwner(context, ownerId)
    }

    private suspend fun renderAndCache(request: SnapshotRequest, widthPx: Int, heightPx: Int): File? {
        val bitmap = withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) { snapshot(request, widthPx, heightPx) } ?: return null
        return withContext(Dispatchers.IO) {
            CourseSnapshotDiskCache.write(context, request, bitmap, widthPx, heightPx, density)
        }
    }

    /** `MapSnapshotter`는 메인 스레드에서 만들고 시작해야 한다. */
    private suspend fun snapshot(request: SnapshotRequest, widthPx: Int, heightPx: Int): Bitmap? =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val styleBuilder = SnapshotStyleBuilder.build(request, routeColor, startColor, startStrokeColor, endpointColor, endpointStrokeColor, newRoadColor, todayRoadColor)
                val options = MapSnapshotter.Options(widthPx, heightPx)
                    .withPixelRatio(density)
                    .withLogo(false)
                    .withAttribution(false)
                    .withStyleBuilder(styleBuilder)
                applyRegion(options, request, widthPx, heightPx)

                val snapshotter = MapSnapshotter(context, options)
                continuation.invokeOnCancellation { snapshotter.cancel() }

                android.util.Log.d("CourseSnapshotDebug", "snapshotter.start()")
                snapshotter.start(
                    object : MapSnapshotter.SnapshotReadyCallback {
                        override fun onSnapshotReady(snapshot: MapSnapshot) {
                            android.util.Log.d("CourseSnapshotDebug", "onSnapshotReady ${snapshot.bitmap.width}x${snapshot.bitmap.height}")
                            if (continuation.isActive) continuation.resumeWith(Result.success(snapshot.bitmap))
                        }
                    },
                    object : MapSnapshotter.ErrorHandler {
                        // 실패 사유(타일 로드, 스타일 로드 등)는 로그에 남기지 않는다 — 좌표가 섞여 들어올 수 있다.
                        override fun onError(error: String) {
                            android.util.Log.d("CourseSnapshotDebug", "onError: $error")
                            if (continuation.isActive) continuation.resumeWith(Result.success(null))
                        }
                    },
                )
            }
        }

    /** `withRegion`은 자체 최대 줌 제한이 없다 — 계산된 줌이 [SnapshotConfig.MAX_ZOOM]을 넘으면
     * 중심점 + 고정 줌([CameraPosition])으로 바꿔 지정한다. */
    private fun applyRegion(options: MapSnapshotter.Options, request: SnapshotRequest, widthPx: Int, heightPx: Int) {
        val fitZoom = request.bounds.zoomToFit(widthPx, heightPx)
        android.util.Log.d("CourseSnapshotDebug", "applyRegion fitZoom=$fitZoom widthPx=$widthPx heightPx=$heightPx")
        if (fitZoom <= SnapshotConfig.MAX_ZOOM) {
            options.withRegion(request.bounds.toLatLngBounds())
        } else {
            val center = request.bounds.toLatLngBounds().center
            options.withCameraPosition(CameraPosition.Builder().target(center).zoom(SnapshotConfig.MAX_ZOOM).build())
        }
    }

    private fun fileKey(request: SnapshotRequest, widthPx: Int, heightPx: Int): String =
        "${request.cacheKeyBase()}_${widthPx}x$heightPx"
}
