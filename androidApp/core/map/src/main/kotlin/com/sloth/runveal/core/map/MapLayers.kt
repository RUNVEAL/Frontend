package com.sloth.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/** 하위 화면이 [RunvealMap]에 그릴 레이어를 선언하는 수단. */
data class MapLayerSpec(val id: String, val geoJson: String)

/** composition에 있는 동안만 [layers]를 [LocalMapState]에 등록하고, 사라지면 해제한다. */
@Composable
fun MapLayers(vararg layers: MapLayerSpec) {
    val mapState = LocalMapState.current
    val specs = layers.toList()
    DisposableEffect(specs) {
        specs.forEach { mapState.setLayer(it.id, it.geoJson) }
        onDispose { specs.forEach { mapState.clearLayer(it.id) } }
    }
}
