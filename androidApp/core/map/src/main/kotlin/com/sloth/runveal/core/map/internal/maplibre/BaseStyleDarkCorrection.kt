package com.sloth.runveal.core.map.internal.maplibre

import android.graphics.Color
import com.sloth.runveal.map.MapConfig
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory

/**
 * 러닝 포커스(zoom >= [MapConfig.ZOOM_IN_THRESHOLD])에서는 기본 다크 스타일의 건물/공원/숲 색이
 * 배경(거의 검정)과 구분이 안 가는 문제를, docs/map/maplibre-tabs-map-style.md §2(MapLibre2 탭)에서
 * 쓴 보정값으로 고친다. 그 아래 줌(동/시군구/시도 뷰)은 원래 스타일 색 그대로 둔다.
 *
 * 홈 지도가 아닌 화면(코스 추천·러닝 중)은 동 뷰로 돌아갈 일이 없으므로 [apply]에
 * `keepZoomedIn = true`를 주면 줌과 무관하게 보정 색으로 고정한다([BaseMapDimmer]와 같은 기준).
 */
internal object BaseStyleDarkCorrection {
    /** 원래 다크 스타일 색(`rgb(10,10,10)`) — zoom 미만에서는 이대로 유지. */
    private const val BUILDING_ORIGINAL = "#0A0A0A"

    /** 원래 다크 스타일 색(`rgb(32,32,32)`) — landuse_park/landcover_wood가 공유한다. */
    private const val PARK_WOOD_ORIGINAL = "#202020"

    /** 네이버 지도 다크모드 실제 색을 픽셀 샘플링해 가져온 보정 색. */
    private const val BUILDING_DARK_FIX = "#34343A"

    /** 구글 탭 다크 스타일의 poi.park 색과 맞춘 보정 색 — park/wood/grass가 공유한다. */
    private const val PARK_WOOD_GRASS_DARK_FIX = "#1F2A1F"

    /** [keepZoomedIn]이면 모든 줌에서 보정 색, 아니면 [MapConfig.ZOOM_IN_THRESHOLD] 이상에서만. */
    fun apply(style: Style, keepZoomedIn: Boolean) {
        // 보정 색으로 바뀌는 줌 — 0이면 모든 줌이 "그 이상"이라 사실상 항상 보정된다.
        val switchZoom = if (keepZoomedIn) 0f else MapConfig.ZOOM_IN_THRESHOLD.toFloat()

        style.getLayerAs<FillLayer>(MapConfig.LAYER_BASE_BUILDING)?.setProperties(
            PropertyFactory.fillColor(colorStep(switchZoom, BUILDING_ORIGINAL, BUILDING_DARK_FIX)),
        )
        style.getLayerAs<FillLayer>(MapConfig.LAYER_BASE_LANDUSE_PARK)?.setProperties(
            PropertyFactory.fillColor(colorStep(switchZoom, PARK_WOOD_ORIGINAL, PARK_WOOD_GRASS_DARK_FIX)),
        )
        style.getLayerAs<FillLayer>(MapConfig.LAYER_BASE_LANDCOVER_WOOD)?.setProperties(
            PropertyFactory.fillColor(colorStep(switchZoom, PARK_WOOD_ORIGINAL, PARK_WOOD_GRASS_DARK_FIX)),
        )
        addGrassLayerIfMissing(style, switchZoom)
        // 이미 추가돼 있던 경우에도 보이는 줌 범위를 지금 기준으로 다시 맞춘다.
        style.getLayerAs<FillLayer>(MapConfig.LAYER_BASE_LANDCOVER_GRASS)?.minZoom = switchZoom
    }

    /**
     * 다크 스타일엔 `class == grass` landcover 레이어가 아예 없다(liberty 스타일에만 있음) — 잔디 녹지가
     * 러닝 포커스에서 안 보이던 문제라, liberty의 같은 레이어 정의(source=openmaptiles, source-layer=
     * landcover, filter class=='grass')를 가져와 다크 보정 색으로 새로 추가한다. [LAYER_BASE_LANDUSE_PARK]
     * 바로 위에 꽂고, 러닝 포커스 전용이라 그 밑 줌에서는 아예 그리지 않는다(minZoom).
     */
    private fun addGrassLayerIfMissing(style: Style, switchZoom: Float) {
        if (style.getLayer(MapConfig.LAYER_BASE_LANDCOVER_GRASS) != null) return

        val grassLayer = FillLayer(MapConfig.LAYER_BASE_LANDCOVER_GRASS, MapConfig.SOURCE_BASE_STYLE).withProperties(
            PropertyFactory.fillColor(Color.parseColor(PARK_WOOD_GRASS_DARK_FIX)),
        )
        grassLayer.setSourceLayer("landcover")
        grassLayer.setFilter(Expression.eq(Expression.get("class"), Expression.literal("grass")))
        grassLayer.minZoom = switchZoom
        style.addLayerAbove(grassLayer, MapConfig.LAYER_BASE_LANDUSE_PARK)
    }

    /** `zoom` 미만은 [originalColor] 그대로, 그 이상(러닝 포커스)은 [fixedColor]로 바뀐다. */
    private fun colorStep(zoom: Float, originalColor: String, fixedColor: String): Expression =
        Expression.step(
            Expression.zoom(),
            Expression.color(Color.parseColor(originalColor)),
            Expression.stop(zoom, Expression.color(Color.parseColor(fixedColor))),
        )
}
