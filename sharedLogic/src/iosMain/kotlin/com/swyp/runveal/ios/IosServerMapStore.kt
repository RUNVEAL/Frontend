package com.swyp.runveal.ios

import com.swyp.runveal.data.local.FileStore
import com.swyp.runveal.network.maps.MapsApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * 서버 지도 파일(`GET /maps/{mapVersion}/...`, `docs/api/API.md` §3.1.2)을 받아 앱 Documents 아래
 * `maps/{mapVersion}/`에 캐시한다 — Android `ServerMapFileStore`의 iOS 버전.
 *
 * 같은 버전의 파일은 절대 안 바뀌므로 이미 받은 파일은 다시 받지 않는다. **필요한 파일만 이름으로
 * 지정해서 받는다** — manifest의 파일을 전부 받으면 전국 `segments-{구코드}.geojson` 255개(약 896MB)를
 * 통째로 내려받게 된다.
 *
 * 받는 파일이 모두 텍스트(`*.json`, `*.geojson`)라 [FileStore](문자열 전용)를 그대로 쓴다.
 * 바이너리(`segments.bin`)를 쓰게 되면 그때 바이트 저장을 따로 붙여야 한다.
 */
@OptIn(ExperimentalForeignApi::class)
class IosServerMapStore(
    private val fileStore: FileStore = FileStore(),
    private val mapsApi: MapsApi = MapsApi(),
) {

    /**
     * [fileNames] 중 아직 없는 것만 내려받고, 파일 이름 → 내용으로 돌려준다. 서버에 없는 파일이면
     * 받기가 실패하므로 호출부가 예외를 처리해야 한다. 쓰기 시작한 버전 외의 폴더는 이때 정리한다.
     */
    suspend fun ensureDownloaded(mapVersion: String, fileNames: List<String>): Map<String, String> {
        deleteOtherVersions(mapVersion)
        val result = mutableMapOf<String, String>()
        for (fileName in fileNames) {
            val relativePath = relativePath(mapVersion, fileName)
            val cached = fileStore.read(relativePath)
            if (cached != null) {
                result[fileName] = cached
                continue
            }
            val text = mapsApi.fetchFile(mapVersion, fileName).decodeToString()
            fileStore.write(relativePath, text)
            result[fileName] = text
        }
        return result
    }

    fun has(mapVersion: String, fileName: String): Boolean =
        fileStore.exists(relativePath(mapVersion, fileName))

    private fun relativePath(mapVersion: String, fileName: String) = "$MAPS_DIR/$mapVersion/$fileName"

    /**
     * 지금 쓰는 버전이 아닌 폴더를 지운다. 서버가 지도를 고쳐 새 `mapVersion`으로 배포하면 폴더가
     * 통째로 새로 생기는데, 예전 폴더는 아무도 다시 안 쓰면서 자리만 차지한다 — 러닝한 구의
     * `segments-{구코드}.geojson`까지 들어 있어서 한 버전에 수백 MB까지 쌓인다.
     */
    private fun deleteOtherVersions(currentVersion: String) {
        val fileManager = NSFileManager.defaultManager
        val root = documentsRoot()?.let { "$it/$MAPS_DIR" } ?: return
        @Suppress("UNCHECKED_CAST")
        val entries = fileManager.contentsOfDirectoryAtPath(root, null) as? List<String> ?: return
        entries.filter { it != currentVersion }.forEach { stale ->
            fileManager.removeItemAtPath("$root/$stale", null)
        }
    }

    private fun documentsRoot(): String? =
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String

    private companion object {
        const val MAPS_DIR = "maps"
    }
}
