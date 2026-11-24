package com.sloth.runveal.core.map.internal.snapshot

import com.sloth.runveal.core.map.SnapshotColors
import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.map.snapshot.SnapshotConfig
import com.sloth.runveal.map.snapshot.SnapshotRequest
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 동 썸네일용 스타일 JSON — 베이스 스타일을 받아 **완성된 한 장**으로 만든다.
 *
 *  - **지명·도로명·경계·수역이름 라벨 제거** — 84dp 그림에 글자가 들어가면 읽히지도 않고 길도 가린다.
 *  - **건물·공원·숲 색을 밝은 쪽으로** — 원본 다크 스타일은 거의 검정(`#0A0A0A`)이라 그림이 통째로
 *    어둡다. 라이브 지도가 러닝 포커스에서 쓰는 보정 색([BaseStyleDarkCorrection])과 같은 값이다.
 *  - **가본 길·동 마스크·동 경계선을 JSON에 직접 넣는다.**
 *
 * **왜 레이어까지 JSON에 넣나**: `Style.Builder`에 `withSource`/`withLayer`로 얹고 `fromJson`을 주면
 * `MapSnapshotter` **생성자 안에서** 스타일 로드 콜백이 동기로 돌고, 그때 native 객체가 아직 준비되지
 * 않아 `nativeAddSource`가 `IllegalStateException: invalid native peer`로 죽는다(`fromUri`는 네트워크
 * 로드가 비동기라 그 타이밍을 비껴간다). 스타일에 처음부터 다 들어 있으면 추가할 게 없어 그 경로를
 * 아예 타지 않는다.
 *
 * 베이스 가공 결과는 한 번 받아 들고 있는다. 못 받으면(오프라인 등) null이고, 호출부는 원본 스타일
 * URL + `withSource` 경로로 떨어진다.
 */
internal object ZoomedInSnapshotStyle {

    /** 라이브 지도의 줌인 보정과 **같은 값**이어야 한다([BaseStyleDarkCorrection]과 함께 고칠 것). */
    private const val BUILDING_DARK_FIX = "#34343A"
    private const val PARK_WOOD_GRASS_DARK_FIX = "#1F2A1F"

    /** 수역 이름 라벨 — `MapConfig`의 숨김 목록에는 없지만 썸네일에서는 글자를 전부 없앤다. */
    private const val WATER_NAME_LAYER_ID = "water_name"

    private val mutex = Mutex()
    private var cachedBase: String? = null
    private var failed = false

    /** 요청에 맞는 완성 스타일 JSON. 베이스를 못 받았으면 null. */
    suspend fun styleJson(request: SnapshotRequest, colors: SnapshotColors): String? {
        val base = baseJson() ?: return null
        return withContext(Dispatchers.Default) { compose(base, request, colors) }
    }

    private suspend fun baseJson(): String? = mutex.withLock {
        cachedBase?.let { return it }
        if (failed) return null
        val built = runCatching { withContext(Dispatchers.IO) { stripAndRecolor() } }.getOrNull()
        if (built == null) failed = true else cachedBase = built
        built
    }

    /** 라벨을 빼고 건물·녹지 색을 밝은 쪽으로 바꾼 베이스. */
    private fun stripAndRecolor(): String {
        val root = JSONObject(URL(MapConfig.STYLE_URL).readText())
        val layers = root.optJSONArray("layers") ?: return root.toString()
        val hidden = (
            MapConfig.BASE_STYLE_PLACE_LABEL_LAYERS +
                MapConfig.BASE_STYLE_ROAD_LABEL_LAYERS +
                MapConfig.BASE_STYLE_BOUNDARY_LAYERS
            ).toSet()

        val kept = JSONArray()
        for (i in 0 until layers.length()) {
            val layer = layers.optJSONObject(i) ?: continue
            val id = layer.optString("id")
            if (id in hidden || id == WATER_NAME_LAYER_ID) continue
            recolorIfNeeded(layer, id)
            kept.put(layer)
        }
        root.put("layers", kept)
        return root.toString()
    }

    /** 줌 보간식을 쓰는 라이브 지도와 달리, 썸네일은 고정 줌 한 장이라 보정 색을 바로 박는다. */
    private fun recolorIfNeeded(layer: JSONObject, id: String) {
        val color = when (id) {
            MapConfig.LAYER_BASE_BUILDING -> BUILDING_DARK_FIX
            MapConfig.LAYER_BASE_LANDUSE_PARK, MapConfig.LAYER_BASE_LANDCOVER_WOOD -> PARK_WOOD_GRASS_DARK_FIX
            else -> return
        }
        val paint = layer.optJSONObject("paint") ?: JSONObject().also { layer.put("paint", it) }
        paint.put("fill-color", color)
    }

    /**
     * 베이스에 이번 요청의 소스·레이어를 얹는다. 순서가 곧 위아래다 —
     * 가본 길 → 동 밖 마스크 → 동 경계선. 마스크가 길보다 위라야 동 밖으로 삐져나간 길이 가려진다.
     */
    private fun compose(base: String, request: SnapshotRequest, colors: SnapshotColors): String {
        val root = JSONObject(base)
        val sources = root.optJSONObject("sources") ?: JSONObject().also { root.put("sources", it) }
        val layers = root.optJSONArray("layers") ?: JSONArray().also { root.put("layers", it) }

        sources.put(SnapshotConfig.ROUTE_SOURCE_ID, geoJsonSource(request.routeGeoJson))
        layers.put(
            lineLayer(
                id = SnapshotConfig.ROUTE_LAYER_ID,
                source = SnapshotConfig.ROUTE_SOURCE_ID,
                color = colors.route,
                widthDp = SnapshotConfig.DONG_ROAD_WIDTH_DP,
            ),
        )

        request.dongMaskGeoJson?.let { mask ->
            sources.put(SnapshotConfig.DONG_MASK_SOURCE_ID, geoJsonSource(mask))
            layers.put(
                JSONObject()
                    .put("id", SnapshotConfig.DONG_MASK_LAYER_ID)
                    .put("type", "fill")
                    .put("source", SnapshotConfig.DONG_MASK_SOURCE_ID)
                    .put("paint", JSONObject().put("fill-color", colors.dongMask.toCssColor())),
            )
        }

        request.dongOutlineGeoJson?.let { outline ->
            sources.put(SnapshotConfig.DONG_OUTLINE_SOURCE_ID, geoJsonSource(outline))
            layers.put(
                lineLayer(
                    id = SnapshotConfig.DONG_OUTLINE_LAYER_ID,
                    source = SnapshotConfig.DONG_OUTLINE_SOURCE_ID,
                    color = colors.dongOutline,
                    widthDp = SnapshotConfig.DONG_OUTLINE_WIDTH_DP,
                ),
            )
        }
        return root.toString()
    }

    private fun geoJsonSource(geoJson: String): JSONObject =
        JSONObject().put("type", "geojson").put("data", JSONObject(geoJson))

    private fun lineLayer(id: String, source: String, color: Int, widthDp: Double): JSONObject =
        JSONObject()
            .put("id", id)
            .put("type", "line")
            .put("source", source)
            .put("layout", JSONObject().put("line-cap", "round").put("line-join", "round"))
            .put("paint", JSONObject().put("line-color", color.toCssColor()).put("line-width", widthDp))

    /** ARGB Int → `#RRGGBB`. 썸네일에 쓰는 색은 모두 불투명이라 알파는 버린다. */
    private fun Int.toCssColor(): String = "#%06X".format(this and 0xFFFFFF)
}
