package com.swyp.runveal.core.map.internal.glow

import android.content.Context
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.glow.DongGlowRenderer
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BOUNDARY_ASSET_PATH = "dong/korea_dong.geojson"

/**
 * 동 하나·단계 하나의 빛 이미지를 메모리 → 디스크 → (없으면) 생성 큐 순서로 구해준다(문서 4장). 화면에
 * 보이는 동 중 **단계 ≥ 1인 동만** [request]를 부르는 건 호출 쪽 책임 — 0단계는 빛이 없다.
 *
 * 생성은 `Dispatchers.Default.limitedParallelism(2)`로 동시 2개까지만 돈다. "화면 중심에 가까운 동부터"
 * (문서 4장)는 별도 우선순위 큐 없이, **호출 쪽이 한 번의 새로고침 안에서 거리 가까운 순으로
 * [request]를 부르는 것**으로 맞춘다 — limitedParallelism 디스패처의 작업 큐가 제출 순서를 대체로
 * 따르므로 이 정도로 충분하다.
 */
internal class DongGlowProvider(private val context: Context) {
    private val memoryCache = DongGlowMemoryCache()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(2))
    private val activeJobs = mutableMapOf<String, Job>()
    private var cacheDir: File? = null

    /**
     * 메모리나 디스크에 이미 있으면 즉시 돌려준다. 없으면 생성 큐에 넣고 `null`을 돌려준다 — 생성이
     * 끝나면 메모리 캐시에 올라가므로, 호출 쪽은 다음 새로고침(카메라 idle)에서 다시 [request]를 불러
     * 결과를 받으면 된다. 준비 전에는 호출 쪽이 이전 이미지를 그대로 둔다(문서 4장).
     */
    suspend fun request(admCd2: String, level: Int, parts: List<DongPolygonPart>): DongGlowAsset? {
        memoryCache.get(admCd2, level)?.let { return it }

        val dir = ensureCacheDir()
        val fromDisk = withContext(Dispatchers.IO) { DongGlowDiskCache.read(dir, admCd2, level) }
        if (fromDisk != null) {
            memoryCache.put(admCd2, level, fromDisk)
            return fromDisk
        }

        val key = jobKey(admCd2, level)
        if (!activeJobs.containsKey(key)) {
            activeJobs[key] = scope.launch {
                try {
                    generateAndCache(dir, admCd2, level, parts)
                } finally {
                    activeJobs.remove(key)
                }
            }
        }
        return null
    }

    /** [stillWantedKeys]([jobKey] 형식)에 없는 대기/진행 중인 생성 작업을 취소한다(문서 4장: 화면을 벗어난 동). */
    fun cancelExcept(stillWantedKeys: Set<String>) {
        activeJobs.keys.filterNot { it in stillWantedKeys }.forEach { key -> activeJobs.remove(key)?.cancel() }
    }

    fun dispose() {
        scope.coroutineContext[Job]?.cancel()
    }

    private suspend fun generateAndCache(dir: File, admCd2: String, level: Int, parts: List<DongPolygonPart>) {
        try {
            val effects = DongGlowEffects.effectsByLevel[level]
            val strokeWidthPx = DongGlowEffects.strokeWidthPxByLevel[level]
            val result = DongGlowRenderer.render(parts, strokeWidthPx, effects)
            val bitmap = result.toBitmap()
            withContext(Dispatchers.IO) { DongGlowDiskCache.write(dir, admCd2, level, bitmap, result.corners) }
            memoryCache.put(admCd2, level, DongGlowAsset(bitmap, result.corners))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "동 빛 생성 실패: admCd2=$admCd2 level=$level", e)
        }
    }

    private suspend fun ensureCacheDir(): File {
        cacheDir?.let { return it }
        return withContext(Dispatchers.IO) {
            val key = DongGlowCacheKey.compute(context, BOUNDARY_ASSET_PATH)
            DongGlowDiskCache.ensureCacheDir(context, key)
        }.also { cacheDir = it }
    }

    companion object {
        private const val TAG = "DongGlowProvider"
        fun jobKey(admCd2: String, level: Int) = "$admCd2:$level"
    }
}
