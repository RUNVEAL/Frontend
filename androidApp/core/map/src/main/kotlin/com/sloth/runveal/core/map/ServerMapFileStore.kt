package com.sloth.runveal.core.map

import android.content.Context
import com.sloth.runveal.network.maps.MapsApi
import java.io.File

/**
 * 서버 지도 파일(`GET /maps/{mapVersion}/...`, `docs/api/API.md` §3.1.2)을 받아 기기 내부
 * 저장소(`context.filesDir/maps/{mapVersion}/`)에 캐시한다. 같은 버전의 파일은 절대 안 바뀌므로
 * 이미 받은 파일은 다시 받지 않는다.
 *
 * **필요한 파일만 이름으로 지정해서 받는다.** 예전엔 manifest의 파일을 전부 받았는데, 실제 서버
 * manifest엔 전국 구별 `segments-{구코드}.geojson`이 다 들어 있어서 255개 파일 896MB를 통째로
 * 내려받았다 — 러닝이 끝날 때마다 그걸 기다리느라 결과 화면이 무한 로딩에 빠졌다.
 */
class ServerMapFileStore(
    private val context: Context,
    private val mapsApi: MapsApi = MapsApi(),
) {
    private fun versionDir(mapVersion: String): File =
        File(context.filesDir, "$MAPS_DIR/$mapVersion").apply { mkdirs() }

    /** [fileNames] 중 아직 없는 것만 내려받고, 그 파일들이 들어있는 디렉터리를 돌려준다. 서버에 없는
     * 파일이면 받기가 실패하므로 호출부가 예외를 처리해야 한다. 쓰기 시작한 버전 외의 폴더는
     * 이 때 정리한다([deleteOtherVersions]). */
    suspend fun ensureDownloaded(mapVersion: String, fileNames: List<String>): File {
        val dir = versionDir(mapVersion)
        deleteOtherVersions(mapVersion)
        for (fileName in fileNames) {
            val file = File(dir, fileName)
            if (file.exists()) continue
            file.writeBytes(mapsApi.fetchFile(mapVersion, fileName))
        }
        return dir
    }

    fun file(mapVersion: String, fileName: String): File = File(versionDir(mapVersion), fileName)

    /**
     * 지금 쓰는 버전이 아닌 폴더를 지운다. 서버가 지도를 고쳐 새 `mapVersion`으로 배포하면 폴더가
     * 통째로 새로 생기는데, 예전 폴더는 아무도 다시 안 쓰면서 자리만 차지한다 — 러닝한 구의
     * `segments-{구코드}.geojson`까지 들어 있어서 실제로 한 버전에 수백 MB까지 쌓였다(2026-11-10 → 896MB).
     *
     * 서버가 `/meta`의 `supported`에서 옛 버전을 빼지 않는 한 되돌아갈 일은 없고, 되돌아가더라도
     * 다시 받으면 되는 캐시라 지워도 안전하다.
     */
    private fun deleteOtherVersions(currentVersion: String) {
        val root = File(context.filesDir, MAPS_DIR)
        root.listFiles()?.forEach { entry ->
            if (entry.isDirectory && entry.name != currentVersion) entry.deleteRecursively()
        }
    }

    private companion object {
        const val MAPS_DIR = "maps"
    }
}
