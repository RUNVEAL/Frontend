package com.sloth.runveal.core.map.internal.snapshot

import com.sloth.runveal.map.MapConfig
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 동 썸네일용 **줌인 스타일** JSON. 베이스 스타일을 받아 두 가지를 고쳐 둔다.
 *
 *  - **지명·도로명·경계 라벨 제거** — 84dp 그림에 글자가 들어가면 읽히지도 않고 길도 가린다.
 *  - **건물·공원·숲 색을 줌인 보정 색으로** — 라이브 지도에서 [BaseStyleDarkCorrection]이 러닝
 *    포커스(줌 14+)에 적용하는 그 색이다. 썸네일은 "줌인한 지도를 동 크기로 본 것"이어야 한다.
 *
 * **왜 JSON을 직접 고치나**: 스냅샷(`MapSnapshotter`)은 스타일이 로드된 뒤 레이어를 만질 훅이 없다 —
 * 라이브 지도처럼 `style.getLayer(...)?.setProperties(...)`를 할 수가 없어서, 로드 **전**에 JSON
 * 단계에서 바꿔 넣는다.
 *
 * 한 번 받아서 메모리에 들고 있는다. 받지 못하면(오프라인 등) null이고, 호출부는 원본 스타일 URL로
 * 떨어진다 — 라벨이 보이고 색 보정이 없지만 그림 자체는 나온다.
 */
internal object ZoomedInSnapshotStyle {

    /** 라이브 지도의 줌인 보정과 **같은 값**이어야 한다([BaseStyleDarkCorrection]과 함께 고칠 것). */
    private const val BUILDING_DARK_FIX = "#34343A"
    private const val PARK_WOOD_GRASS_DARK_FIX = "#1F2A1F"

    private val mutex = Mutex()
    private var cached: String? = null
    private var failed = false

    suspend fun jsonOrNull(): String? = mutex.withLock {
        cached?.let { return it }
        if (failed) return null
        val built = runCatching { withContext(Dispatchers.IO) { build() } }.getOrNull()
        if (built == null) failed = true else cached = built
        built
    }

    private fun build(): String {
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
            // 지명·도로명·경계선은 통째로 뺀다. 수역 이름도 글자라 같이 뺀다.
            if (id in hidden || id == WATER_NAME_LAYER_ID) continue
            recolorIfNeeded(layer, id)
            kept.put(layer)
        }
        root.put("layers", kept)
        return root.toString()
    }

    /** 줌 보간식(`colorStep`)을 쓰는 라이브 지도와 달리, 썸네일은 고정 줌 한 장이라 보정 색을 바로 박는다. */
    private fun recolorIfNeeded(layer: JSONObject, id: String) {
        val color = when (id) {
            MapConfig.LAYER_BASE_BUILDING -> BUILDING_DARK_FIX
            MapConfig.LAYER_BASE_LANDUSE_PARK, MapConfig.LAYER_BASE_LANDCOVER_WOOD -> PARK_WOOD_GRASS_DARK_FIX
            else -> return
        }
        val paint = layer.optJSONObject("paint") ?: JSONObject().also { layer.put("paint", it) }
        paint.put("fill-color", color)
    }

    /** 수역 이름 라벨 — `MapConfig`의 숨김 목록에는 없지만 썸네일에서는 글자를 전부 없앤다. */
    private const val WATER_NAME_LAYER_ID = "water_name"
}
