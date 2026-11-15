package com.swyp.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swyp.runveal.core.map.internal.maplibre.MapLibreMapView
import com.swyp.runveal.dong.DongPolygonPart

/**
 * feature가 아는 것은 이것뿐 — MapLibre SDK 타입은 전부 internal에 숨긴다. Map Shell에서 단 하나만 그리고,
 * 하위 화면은 [LocalMapState]로 상태를 읽거나 [MapLayers]로 레이어를 선언한다.
 */
@Composable
fun RunvealMap(
    modifier: Modifier = Modifier,
    state: MapState = rememberMapState(),
    dongOverlay: DongOverlayGeoJson? = null,
    /** 동 빛 이미지 생성([com.swyp.runveal.dong.glow.DongGlowRenderer])에 쓰는, 구멍을 보존한 전국 동 폴리곤. */
    dongGlowPolygonParts: Map<String, List<DongPolygonPart>> = emptyMap(),
    /** [dongGlowPolygonParts]가 어느 경계 데이터에서 나왔는지(`"asset"` / `"server:<지도 버전>"`).
     * 빛 이미지 캐시 키에 들어가, 서버가 경계를 고쳐 배포하면 옛 이미지를 쓰지 않는다. */
    dongBoundarySourceKey: String = "",
    sigunguOverlay: SigunguOverlayGeoJson? = null,
    sidoOverlay: SidoOverlayGeoJson? = null,
) {
    MapLibreMapView(
        modifier = modifier,
        state = state,
        dongOverlay = dongOverlay,
        dongGlowPolygonParts = dongGlowPolygonParts,
        dongBoundarySourceKey = dongBoundarySourceKey,
        sigunguOverlay = sigunguOverlay,
        sidoOverlay = sidoOverlay,
    )
}
