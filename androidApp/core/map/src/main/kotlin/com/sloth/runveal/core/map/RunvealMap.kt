package com.sloth.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sloth.runveal.core.map.internal.maplibre.MapLibreMapView
import com.sloth.runveal.dong.DongPolygonPart

/**
 * feature가 아는 것은 이것뿐 — MapLibre SDK 타입은 전부 internal에 숨긴다. Map Shell에서 단 하나만 그리고,
 * 하위 화면은 [LocalMapState]로 상태를 읽거나 [MapLayers]로 레이어를 선언한다.
 */
@Composable
fun RunvealMap(
    modifier: Modifier = Modifier,
    state: MapState = rememberMapState(),
    dongOverlay: DongOverlayGeoJson? = null,
    /** 동 빛 이미지 생성([com.sloth.runveal.dong.glow.DongGlowRenderer])에 쓰는, 구멍을 보존한 전국 동 폴리곤. */
    dongGlowPolygonParts: Map<String, List<DongPolygonPart>> = emptyMap(),
    /** [dongGlowPolygonParts]가 어느 경계 데이터에서 나왔는지(`"asset"` / `"server:<지도 버전>"`).
     * 빛 이미지 캐시 키에 들어가, 서버가 경계를 고쳐 배포하면 옛 이미지를 쓰지 않는다. */
    dongBoundarySourceKey: String = "",
    sigunguOverlay: SigunguOverlayGeoJson? = null,
    sidoOverlay: SidoOverlayGeoJson? = null,
    /** 위치 권한이 허용돼 있는지. 지도는 권한을 받기 전에 떠 있고(`RequireLocationPermission`이 지도
     * 위에 안내 시트를 띄운다) 허용은 그 뒤에 일어나므로, 이 값이 true로 바뀌는 순간을 보고 현재
     * 위치 점을 켠다. 기본값이 false라 안 넘기면 내 위치가 안 뜬다. */
    locationPermissionGranted: Boolean = false,
) {
    MapLibreMapView(
        modifier = modifier,
        state = state,
        dongOverlay = dongOverlay,
        dongGlowPolygonParts = dongGlowPolygonParts,
        dongBoundarySourceKey = dongBoundarySourceKey,
        sigunguOverlay = sigunguOverlay,
        sidoOverlay = sidoOverlay,
        locationPermissionGranted = locationPermissionGranted,
    )
}
