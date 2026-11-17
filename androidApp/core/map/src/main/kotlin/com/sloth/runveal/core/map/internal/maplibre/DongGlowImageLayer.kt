package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.core.map.internal.glow.DongGlowProvider
import com.sloth.runveal.dong.DongPolygonPart
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.MapConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.ImageSource
import org.maplibre.geojson.FeatureCollection

/**
 * `docs/DONG_GLOW_ON_DEVICE.md` Step 4 — 동마다 [DongGlowProvider]가 구해준 빛 이미지를
 * `ImageSource`+`RasterLayer`로 `dong-fill`과 `dong-line` 사이에 올린다(`docs/DONG_GLOW_IMAGE_APPLY.md`
 * 3장 레이어 구성). 계산·캐시는 전부 [DongGlowProvider] 몫이고, 여기서는 화면에 보이는 동을 추려서
 * 요청하고 결과를 지도에 얹기만 한다.
 */
internal class DongGlowImageLayer(private val provider: DongGlowProvider) {
    private val activeAdmCd2 = mutableSetOf<String>()
    private var cachedFillGeoJson: String? = null
    private var cachedAdmCd2Levels: Map<String, Int> = emptyMap()

    /**
     * [overlayFillGeoJson]에서 동별 단계(0~6)를 뽑고, [polygonPartsByAdmCd2](전국)에서 그중 단계 ≥ 1이고
     * 화면에 보이는 동만 추려 [DongGlowProvider.request]를 부른다. 화면 중심에 가까운 순으로 부르고,
     * 더 이상 보이지 않는 동은 레이어를 지우고 대기 중인 생성 작업도 취소한다.
     */
    suspend fun refresh(
        style: Style,
        map: MapLibreMap,
        overlayFillGeoJson: String?,
        polygonPartsByAdmCd2: Map<String, List<DongPolygonPart>>,
        boundarySourceKey: String,
    ) {
        // 러닝 기록 중·코스 추천 중처럼 동 오버레이를 숨길 때는 parts/overlay가 비어서 들어온다. 그냥
        // 돌아가면 테두리·라벨만 사라지고 **이미 올라간 빛 이미지는 지도에 그대로 남는다** — 올라가 있던
        // 것까지 같이 내린다.
        val admCd2Levels = if (polygonPartsByAdmCd2.isEmpty()) emptyMap() else admCd2LevelsFor(overlayFillGeoJson)
        if (admCd2Levels.isEmpty()) {
            removeAllDongs(style)
            return
        }

        val visibleBounds = map.projection.visibleRegion.latLngBounds
        // target이 null인 건 사실상 없지만(스타일 로드 후 카메라는 항상 설정돼 있다) 타입상 nullable이라
        // 화면 중심을 bounds로 다시 계산해 둔다.
        val cameraCenter = map.cameraPosition.target ?: LatLng(
            (visibleBounds.northEast.latitude + visibleBounds.southWest.latitude) / 2,
            (visibleBounds.northEast.longitude + visibleBounds.southWest.longitude) / 2,
        )

        val candidates = admCd2Levels.entries
            .filter { (_, level) -> level >= 1 }
            .mapNotNull { (admCd2, level) ->
                val parts = polygonPartsByAdmCd2[admCd2] ?: return@mapNotNull null
                val bounds = boundsOf(parts) ?: return@mapNotNull null
                if (!intersects(visibleBounds, bounds)) return@mapNotNull null
                Triple(admCd2, level, distanceSqTo(cameraCenter, bounds))
            }
            .sortedBy { it.third } // 화면 중심에 가까운 동부터(문서 4장)

        if (candidates.size > VISIBLE_COUNT_WARNING_THRESHOLD) {
            android.util.Log.w(TAG, "한 번에 ${candidates.size}개 동 빛 이미지가 화면에 걸림 (주의 기준 $VISIBLE_COUNT_WARNING_THRESHOLD)")
        }

        provider.cancelExcept(candidates.map { (admCd2, level, _) -> DongGlowProvider.jobKey(admCd2, level) }.toSet())

        val targetAdmCd2 = candidates.map { it.first }.toSet()
        (activeAdmCd2 - targetAdmCd2).forEach { admCd2 -> removeDong(style, admCd2); activeAdmCd2.remove(admCd2) }

        for ((admCd2, level, _) in candidates) {
            val parts = polygonPartsByAdmCd2.getValue(admCd2)
            val asset = provider.request(admCd2, level, parts, boundarySourceKey) ?: continue
            addOrUpdateDong(style, admCd2, asset.corners, asset.bitmap)
            activeAdmCd2.add(admCd2)
        }
    }

    private fun addOrUpdateDong(style: Style, admCd2: String, corners: List<LatLon>, bitmap: android.graphics.Bitmap) {
        val quad = LatLngQuad(corners[0].toLatLng(), corners[1].toLatLng(), corners[2].toLatLng(), corners[3].toLatLng())
        val sourceId = sourceId(admCd2)
        val existingSource = style.getSourceAs<ImageSource>(sourceId)
        if (existingSource != null) {
            existingSource.setImage(bitmap)
            existingSource.setCoordinates(quad)
        } else {
            style.addSource(ImageSource(sourceId, quad, bitmap))
        }

        val layerId = layerId(admCd2)
        if (style.getLayer(layerId) == null) {
            val layer = RasterLayer(layerId, sourceId).withProperties(
                PropertyFactory.rasterResampling(Property.RASTER_RESAMPLING_LINEAR),
                PropertyFactory.rasterFadeDuration(0f),
            )
            layer.minZoom = MapConfig.SIGUNGU_SWITCH_ZOOM.toFloat()
            layer.maxZoom = MapConfig.ZOOM_IN_THRESHOLD.toFloat()
            // 3장 레이어 순서: dong-fill 위, dong-line 아래.
            if (style.getLayer(MapConfig.LAYER_DONG_LINE) != null) {
                style.addLayerBelow(layer, MapConfig.LAYER_DONG_LINE)
            } else {
                style.addLayer(layer)
            }
        }
    }

    /** 올라가 있는 동 빛 이미지를 전부 내리고 대기 중인 생성 작업도 취소한다. */
    private fun removeAllDongs(style: Style) {
        if (activeAdmCd2.isEmpty()) return
        provider.cancelExcept(emptySet())
        activeAdmCd2.toList().forEach { admCd2 -> removeDong(style, admCd2) }
        activeAdmCd2.clear()
    }

    private fun removeDong(style: Style, admCd2: String) {
        style.getLayer(layerId(admCd2))?.let { style.removeLayer(it) }
        style.getSource(sourceId(admCd2))?.let { style.removeSource(it) }
    }

    private fun sourceId(admCd2: String) = "dong-glow-img-src-$admCd2"
    private fun layerId(admCd2: String) = "dong-glow-img-$admCd2"
    private fun LatLon.toLatLng() = LatLng(lat, lon)

    private fun boundsOf(parts: List<DongPolygonPart>): LatLonBounds? {
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        var any = false
        for (part in parts) {
            for (p in part.exterior) {
                any = true
                if (p.lat < minLat) minLat = p.lat
                if (p.lat > maxLat) maxLat = p.lat
                if (p.lon < minLon) minLon = p.lon
                if (p.lon > maxLon) maxLon = p.lon
            }
        }
        return if (any) LatLonBounds(minLat, maxLat, minLon, maxLon) else null
    }

    private fun intersects(visible: LatLngBounds, bounds: LatLonBounds): Boolean {
        val east = visible.northEast.longitude
        val west = visible.southWest.longitude
        val north = visible.northEast.latitude
        val south = visible.southWest.latitude
        return bounds.minLon <= east && bounds.maxLon >= west && bounds.minLat <= north && bounds.maxLat >= south
    }

    private fun distanceSqTo(center: LatLng, bounds: LatLonBounds): Double {
        val cLat = (bounds.minLat + bounds.maxLat) / 2
        val cLon = (bounds.minLon + bounds.maxLon) / 2
        val dLat = center.latitude - cLat
        val dLon = center.longitude - cLon
        return dLat * dLat + dLon * dLon
    }

    /** [overlayFillGeoJson]은 한 화면 세션 동안 바뀌지 않는 동일 문자열이 반복해서 들어오므로(== 참조
     * 비교로 충분), 매 새로고침마다 다시 파싱하지 않는다 — 과거 이 재파싱이 팬/줌 버벅임의 원인이었다. */
    private suspend fun admCd2LevelsFor(fillGeoJson: String?): Map<String, Int> {
        if (fillGeoJson == null) return emptyMap()
        if (fillGeoJson === cachedFillGeoJson) return cachedAdmCd2Levels
        val parsed = withContext(Dispatchers.Default) { parseAdmCd2Levels(fillGeoJson) }
        cachedFillGeoJson = fillGeoJson
        cachedAdmCd2Levels = parsed
        return parsed
    }

    private fun parseAdmCd2Levels(fillGeoJson: String): Map<String, Int> {
        val features = FeatureCollection.fromJson(fillGeoJson).features() ?: return emptyMap()
        val result = mutableMapOf<String, Int>()
        features.forEach { feature ->
            val admCd2 = feature.getStringProperty("adm_cd2") ?: return@forEach
            val level = feature.getNumberProperty("level")?.toInt() ?: return@forEach
            result[admCd2] = level
        }
        return result
    }

    private data class LatLonBounds(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    private companion object {
        const val TAG = "DongGlowImageLayer"
        const val VISIBLE_COUNT_WARNING_THRESHOLD = 60
    }
}
