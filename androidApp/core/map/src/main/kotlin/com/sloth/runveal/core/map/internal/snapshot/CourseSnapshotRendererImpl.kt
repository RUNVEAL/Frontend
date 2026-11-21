package com.sloth.runveal.core.map.internal.snapshot

import android.content.Context
import android.graphics.Bitmap
import com.sloth.runveal.core.map.CourseSnapshotRenderer
import com.sloth.runveal.core.map.SnapshotColors
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
 * 출발·도착 점 반지름 = 그림 높이 x 이 비율. 디자인시스템 마커(지름 18dp) 크기로 그려 봤더니
 * 썸네일 안에서는 너무 작아 보여서 **지름 기준 2.5배**로 키웠다 — 160dp 카드에서 반지름 17.5dp(지름 35dp).
 */
private const val ENDPOINT_RADIUS_RATIO = 17.5f / 160f

/** 아주 작은 썸네일에서도 점이 보이게, 반대로 큰 그림에서 과하게 커지지 않게 두는 한계. */
private const val MIN_ENDPOINT_RADIUS_DP = 11.25f
private const val MAX_ENDPOINT_RADIUS_DP = 20f

/**
 * [CourseSnapshotRenderer] 구현. [colors]는 Compose 컨텍스트가 없는 이 레이어에 Composable 쪽
 * ([CourseSnapshotImage])이 [MapColors]와 같은 패턴으로 미리 ARGB로 바꿔 넘긴다.
 */
internal class CourseSnapshotRendererImpl(
    private val context: Context,
    private val colors: SnapshotColors,
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

    /**
     * 출발·도착 점 반지름(dp) — 그림 높이에 비례시킨다. 같은 dp로 고정하면 160dp 카드에선 작아 보이고
     * 72dp 줄 썸네일에선 그림을 다 덮는다.
     */
    private fun endpointRadiusDp(heightPx: Int): Float {
        val heightDp = heightPx / density
        return (heightDp * ENDPOINT_RADIUS_RATIO).coerceIn(MIN_ENDPOINT_RADIUS_DP, MAX_ENDPOINT_RADIUS_DP)
    }

    /** `MapSnapshotter`는 메인 스레드에서 만들고 시작해야 한다. */
    private suspend fun snapshot(request: SnapshotRequest, widthPx: Int, heightPx: Int): Bitmap? =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val styleBuilder = SnapshotStyleBuilder.build(
                    request = request,
                    colors = colors,
                    endpointRadiusDp = endpointRadiusDp(heightPx),
                )
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
