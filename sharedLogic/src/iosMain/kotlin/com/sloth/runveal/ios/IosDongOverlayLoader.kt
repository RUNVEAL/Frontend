package com.sloth.runveal.ios

import com.sloth.runveal.data.local.FileStore
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.data.local.TrackLocalStore
import com.sloth.runveal.dong.DongExploreRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 번들에 들어 있는 경계 에셋 원본 텍스트 — iOS 번들에서 읽는 건 Swift(`RunvealBundleAsset`) 몫이고,
 * 여기서는 넘겨받은 텍스트만 쓴다(Kotlin이 iOS 번들 구조를 알 필요가 없게).
 *
 * **동 경계는 여기 없다.** 서버 `index.geojson`에서만 온다([IosServerDongData]). 시군구·시도 경계와
 * 계층은 서버가 주지 않는 자료라 번들에 남아 있고, 서버 `index.geojson`에서 생성해 코드 체계를
 * 맞춰 뒀다(`androidApp/core/map/src/main/assets/dong/SOURCE.md`).
 */
data class DongBundleAssets(
    val sigunguGeoJson: String,
    val sidoGeoJson: String,
    val regionHierarchyJson: String,
)

/**
 * 내 기록 기준 동/시군구/시도 오버레이를 만든다 — Android `core:map`의 `loadDongOverlayBundle`에 해당.
 *
 * 분자는 실제로 달려서 "가본 길"이 된 구간 길이(`explored_segment`), 분모는 그 동 전체 도로 길이다 —
 * 서버 `index.json`을 받았으면 그 값을, 비어 있으면 로컬에 들여온 구간 인덱스 집계를 쓴다.
 */
object IosDongOverlayLoader {

    /**
     * [Throws]가 없으면 DB·파싱 중 난 예외가 Swift `catch`로 가지 않고 앱을 종료시킨다
     * ([IosMeta.fetchAndPublish] 주석 참고). 홈 지도는 오버레이를 못 만들어도 떠야 한다.
     *
     * [Dispatchers.IO]로 옮기는 이유는 Android `loadDongOverlayBundle`과 같다 — 전국 경계 GeoJSON
     * 파싱과 동별 집계에 몇 초가 걸리는데, 안 옮기면 Kotlin/Native가 **부른 쪽 스레드에서 그대로**
     * 돌려서 호출부가 메인 액터면 그만큼 화면이 멈춘다.
     */
    @Throws(Exception::class)
    suspend fun load(assets: DongBundleAssets): DongOverlayGeoJsonBundle = withContext(Dispatchers.IO) {
        val trackLocalStore = TrackLocalStore(RunvealDatabaseProvider.database, FileStore())

        // 동 경계는 서버에서만 온다. 못 받으면(오프라인 등) 동 레이어를 비운 채 돌려준다 —
        // 번들 스냅샷으로 대신 그리면 서버와 코드 체계가 달라 완주율이 조용히 틀어진다.
        val server = IosServerDongData.fetch() ?: return@withContext DongOverlayGeoJsonBundle.empty()

        val walkedLengths = trackLocalStore.walkedLengthByDong()
        val totalLengths = server.totalLengthsByDong.ifEmpty { trackLocalStore.totalLengthByDong() }
        val dongPercents = DongExploreRate.percents(walkedLengths, totalLengths)

        IosDongOverlayBuilder.build(
            dongBoundaryGeoJson = server.boundaryGeoJson,
            sigunguBoundaryGeoJson = assets.sigunguGeoJson,
            sidoBoundaryGeoJson = assets.sidoGeoJson,
            regionHierarchyJson = assets.regionHierarchyJson,
            dongPercents = dongPercents,
            walkedLengthsByDong = walkedLengths,
            totalLengthsByDong = totalLengths,
            boundarySourceKey = "server:${server.mapVersion}",
        )
    }
}
