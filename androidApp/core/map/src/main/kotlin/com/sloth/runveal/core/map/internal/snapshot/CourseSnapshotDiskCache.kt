package com.sloth.runveal.core.map.internal.snapshot

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.sloth.runveal.map.snapshot.SnapshotConfig
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import java.io.File

/** `filesDir/course_snapshots/{variant}_{ownerId}_{STYLE_VERSION}_{w}x{h}@{ratio}.webp`
 * ([DongGlowDiskCache]와 같은 파일 캐시 패턴 — `docs/running/COURSE_MAP_SNAPSHOT.md`는 `cacheDir`를
 * 예로 들지만, 이 프로젝트의 기존 이미지 캐시는 전부 `filesDir`를 쓰므로 그 관례를 따른다). 모든 함수는
 * 블로킹 I/O라 호출 쪽에서 `Dispatchers.IO`로 불러야 한다. */
internal object CourseSnapshotDiskCache {
    private const val ROOT_DIR = "course_snapshots"
    private const val WEBP_QUALITY = 90

    fun file(context: Context, request: SnapshotRequest, widthPx: Int, heightPx: Int, pixelRatio: Float): File =
        File(rootDir(context), fileName(request, widthPx, heightPx, pixelRatio))

    /** 임시 파일에 쓴 뒤 rename해서 원자적으로 저장하고, 같은 [SnapshotRequest.cacheKeyBase]의 이전
     * 버전(크기·density·STYLE_VERSION이 달랐던) 파일은 지운다. */
    fun write(context: Context, request: SnapshotRequest, bitmap: Bitmap, widthPx: Int, heightPx: Int, pixelRatio: Float): File {
        val dir = rootDir(context).apply { if (!exists()) mkdirs() }
        val name = fileName(request, widthPx, heightPx, pixelRatio)
        val target = File(dir, name)
        val temp = File(dir, "$name.tmp")
        temp.outputStream().use { out -> bitmap.compress(webpFormat(), WEBP_QUALITY, out) }
        temp.renameTo(target)
        deleteStaleVersions(dir, request.cacheKeyBase(), keepFileName = name)
        return target
    }

    fun deleteAllForOwner(context: Context, ownerId: String) {
        val dir = rootDir(context)
        val prefixes = SnapshotVariant.entries.map { "${it.name.lowercase()}_${ownerId}_" }
        dir.listFiles()?.forEach { file -> if (prefixes.any { file.name.startsWith(it) }) file.delete() }
    }

    private fun deleteStaleVersions(dir: File, cacheKeyBase: String, keepFileName: String) {
        val prefix = "${cacheKeyBase}_"
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefix) && file.name != keepFileName) file.delete()
        }
    }

    private fun rootDir(context: Context): File = File(context.filesDir, ROOT_DIR)

    private fun fileName(request: SnapshotRequest, widthPx: Int, heightPx: Int, pixelRatio: Float): String =
        "${request.cacheKeyBase()}_${SnapshotConfig.STYLE_VERSION}_${widthPx}x${heightPx}@$pixelRatio.webp"

    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
}
