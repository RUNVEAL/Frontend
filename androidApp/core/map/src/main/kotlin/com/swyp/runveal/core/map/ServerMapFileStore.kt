package com.swyp.runveal.core.map

import android.content.Context
import com.swyp.runveal.network.maps.MapsApi
import java.io.File

/**
 * 서버 지도 파일(`GET /maps/{mapVersion}/...`, `docs/api/API.md` §3.1.2/§8.3)을 받아 기기 내부
 * 저장소(`context.filesDir/maps/{mapVersion}/`)에 캐시한다. 같은 버전의 파일은 절대 안 바뀌므로
 * 이미 받은 파일은 다시 받지 않는다 — manifest만 매번 확인해서 새로 생긴 파일이 있으면 그것만 받는다.
 */
class ServerMapFileStore(
    private val context: Context,
    private val mapsApi: MapsApi = MapsApi(),
) {
    private fun versionDir(mapVersion: String): File =
        File(context.filesDir, "maps/$mapVersion").apply { mkdirs() }

    /** [mapVersion]의 manifest를 받아 아직 없는 파일만 내려받고, 전부 들어있는 디렉터리를 돌려준다. */
    suspend fun ensureDownloaded(mapVersion: String): File {
        val dir = versionDir(mapVersion)
        val manifest = mapsApi.fetchManifest(mapVersion)
        for (fileName in manifest.files) {
            val file = File(dir, fileName)
            if (file.exists()) continue
            file.writeBytes(mapsApi.fetchFile(mapVersion, fileName))
        }
        return dir
    }

    fun file(mapVersion: String, fileName: String): File = File(versionDir(mapVersion), fileName)
}
