package com.sloth.runveal.core.map.internal.glow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.sloth.runveal.map.LatLon
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** 비트맵 + 네 꼭짓점(위경도). [DongGlowDiskCache]와 메모리 캐시가 같이 들고 다니는 단위. */
internal data class DongGlowAsset(val bitmap: Bitmap, val corners: List<LatLon>)

@Serializable
internal data class DongGlowAssetJson(val corners: List<List<Double>>, val width: Int, val height: Int)

/**
 * `filesDir/dong_glow/{cacheKey}/{adm_cd2}_{level}.webp` + 같은 이름의 `.json`(corners, size)(문서 4장).
 * 모든 함수는 블로킹 I/O라 호출 쪽에서 `Dispatchers.IO`로 불러야 한다.
 */
internal object DongGlowDiskCache {
    private const val ROOT_DIR = "dong_glow"
    private const val WEBP_QUALITY = 85
    private val json = Json { ignoreUnknownKeys = true }

    /** [cacheKey] 폴더가 없으면 만들고, 다른 키로 남아있던 이전 폴더는 지운다. */
    fun ensureCacheDir(context: Context, cacheKey: String): File {
        val root = File(context.filesDir, ROOT_DIR)
        val dir = File(root, cacheKey)
        if (!dir.exists()) {
            root.listFiles()?.forEach { stale -> if (stale.name != cacheKey) stale.deleteRecursively() }
            dir.mkdirs()
        }
        return dir
    }

    fun read(dir: File, admCd2: String, level: Int): DongGlowAsset? {
        val webp = File(dir, "${fileBase(admCd2, level)}.webp")
        val sidecar = File(dir, "${fileBase(admCd2, level)}.json")
        if (!webp.exists() || !sidecar.exists()) return null
        val bitmap = BitmapFactory.decodeFile(webp.path) ?: return null
        val meta = try {
            json.decodeFromString<DongGlowAssetJson>(sidecar.readText())
        } catch (_: Exception) {
            return null
        }
        return DongGlowAsset(bitmap, meta.corners.map { LatLon(lat = it[1], lon = it[0]) })
    }

    fun write(dir: File, admCd2: String, level: Int, bitmap: Bitmap, corners: List<LatLon>) {
        val webp = File(dir, "${fileBase(admCd2, level)}.webp")
        webp.outputStream().use { out -> bitmap.compress(webpFormat(), WEBP_QUALITY, out) }

        val sidecar = File(dir, "${fileBase(admCd2, level)}.json")
        val meta = DongGlowAssetJson(
            corners = corners.map { listOf(it.lon, it.lat) },
            width = bitmap.width,
            height = bitmap.height,
        )
        sidecar.writeText(json.encodeToString(DongGlowAssetJson.serializer(), meta))
    }

    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

    private fun fileBase(admCd2: String, level: Int) = "${admCd2}_$level"
}
