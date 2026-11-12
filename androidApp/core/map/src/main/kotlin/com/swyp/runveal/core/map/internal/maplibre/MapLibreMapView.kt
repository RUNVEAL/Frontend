package com.swyp.runveal.core.map.internal.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.swyp.runveal.core.map.DongOverlayGeoJson
import com.swyp.runveal.core.map.MapState
import com.swyp.runveal.core.map.SidoOverlayGeoJson
import com.swyp.runveal.core.map.SigunguOverlayGeoJson
import com.swyp.runveal.core.map.internal.glow.DongGlowProvider
import com.swyp.runveal.designsystem.component.map.RunvealMapMarker
import com.swyp.runveal.designsystem.component.map.RunvealMapMarkerStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.MapConfig
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlin.math.roundToInt

/** [LatLngBounds]로 카메라를 맞출 때 코스 선이 화면 가장자리에 딱 붙지 않게 주는 여백. */
private const val BOUNDS_FOCUS_PADDING_DP = 48f

@Composable
internal fun MapLibreMapView(
    modifier: Modifier,
    state: MapState,
    dongOverlay: DongOverlayGeoJson?,
    dongGlowPolygonParts: Map<String, List<DongPolygonPart>>,
    sigunguOverlay: SigunguOverlayGeoJson?,
    sidoOverlay: SidoOverlayGeoJson?,
) {
    val mapView = rememberMapViewWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    var style by remember { mutableStateOf<Style?>(null) }
    var liveMap by remember { mutableStateOf<MapLibreMap?>(null) }
    // 카메라가 멈출 때마다(OnCameraIdle) 화면에 보이는 동 빛 이미지를 다시 계산하기 위한 트리거.
    var cameraIdleTick by remember { mutableIntStateOf(0) }
    val dongGlowProvider = remember { DongGlowProvider(context) }
    val dongGlowImageLayer = remember { DongGlowImageLayer(dongGlowProvider) }
    DisposableEffect(Unit) { onDispose { dongGlowProvider.dispose() } }
    val colors = RunvealTheme.colors
    val mapColors = remember(colors) {
        MapColors(
            visitedRoadGlow = colors.accent.glow.toArgb(),
            visitedRoadCore = colors.map.roadExplored.toArgb(),
            currentLocation = colors.map.location.toArgb(),
            dongLabelPrimary = colors.text.primary.toArgb(),
            dongLabelSecondary = colors.text.secondary.toArgb(),
            sigunguLabel = colors.text.secondary.toArgb(),
            recommendedRouteAlt = colors.map.routeAlt.toArgb(),
            recommendedRouteSelectedGlow = colors.map.roadNewHalo.toArgb(),
            recommendedRouteSelected = colors.map.roadNew.toArgb(),
        )
    }

    var pinScreenOffset by remember { mutableStateOf<Offset?>(null) }
    fun refreshPinScreenOffset() {
        val map = liveMap
        val pinLocation = state.pin?.location
        pinScreenOffset = if (map == null || pinLocation == null) {
            null
        } else {
            map.projection.toScreenLocation(pinLocation.toMapLibreLatLng()).let { Offset(it.x, it.y) }
        }
    }

    Box(modifier = modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView }) { view ->
            view.getMapAsync { map ->
                liveMap = map
                if (style == null) {
                    setUpInitialCamera(map, state)
                    map.addOnCameraIdleListener {
                        state.currentZoom = map.cameraPosition.zoom
                        cameraIdleTick++
                    }
                    map.addOnCameraMoveListener { refreshPinScreenOffset() }
                    map.addOnMapClickListener { latLng ->
                        state.lastMapClick = LatLon(latLng.latitude, latLng.longitude)
                        true
                    }
                    map.setStyle(MapConfig.STYLE_URL) { loadedStyle ->
                        style = loadedStyle
                        // 베이스 스타일 레이어 스냅샷이 필요해서 다른 레이어를 추가하기 전에 가장 먼저 호출한다.
                        BaseMapDimmer.applyAtStyleLoad(loadedStyle)
                        BaseStyleDarkCorrection.applyAtStyleLoad(loadedStyle)
                        CurrentLocationLayer.activate(view.context, map, loadedStyle, mapColors.currentLocation)
                        BaseStyleLabels.hide(loadedStyle)
                    }
                }
            }
        }

        LaunchedEffect(state.pin, liveMap) { refreshPinScreenOffset() }

        val pin = state.pin
        val screenOffset = pinScreenOffset
        if (pin != null && screenOffset != null) {
            // Pin 디자인의 앵커(바닥 점의 중심)는 Column 맨 아래 8dp 점의 중앙이라 가로는 절반,
            // 세로는 (전체 높이 - 점 반지름)만큼 끌어올려야 탭한 좌표와 점이 겹친다.
            val dotRadiusPx = with(density) { 4.dp.toPx() }
            var pinSize by remember { mutableStateOf(IntSize.Zero) }
            Box(
                modifier = Modifier
                    .onSizeChanged { pinSize = it }
                    .offset {
                        IntOffset(
                            x = (screenOffset.x - pinSize.width / 2f).roundToInt(),
                            y = (screenOffset.y - pinSize.height + dotRadiusPx).roundToInt(),
                        )
                    },
            ) {
                RunvealMapMarker(type = pin.type, style = RunvealMapMarkerStyle.Pin)
            }
        }
    }

    val visitedRoadGeoJson = state.layers[MapConfig.SOURCE_VISITED_ROAD]
    LaunchedEffect(style, visitedRoadGeoJson, mapColors) {
        style?.let { VisitedRoadLayer.apply(it, visitedRoadGeoJson, mapColors) }
    }
    val recommendedRouteGeoJson = state.layers[MapConfig.SOURCE_RECOMMENDED_ROUTE]
    LaunchedEffect(style, recommendedRouteGeoJson, mapColors) {
        style?.let { RecommendedRouteLayer.apply(it, recommendedRouteGeoJson, mapColors) }
    }
    LaunchedEffect(style, dongOverlay, mapColors) {
        style?.let { DongOverlayLayer.apply(it, dongOverlay, mapColors) }
    }
    // 동 빛 이미지. dongOverlay(단계 변경)나 카메라가 멈출 때(화면 범위 변경) 둘 중 하나라도 바뀌면
    // 다시 계산한다(docs/DONG_GLOW_ON_DEVICE.md Step 4).
    LaunchedEffect(style, liveMap, dongOverlay, dongGlowPolygonParts, cameraIdleTick) {
        val currentStyle = style ?: return@LaunchedEffect
        val currentMap = liveMap ?: return@LaunchedEffect
        dongGlowImageLayer.refresh(currentStyle, currentMap, dongOverlay?.fillGeoJson, dongGlowPolygonParts)
    }
    LaunchedEffect(style, sigunguOverlay, mapColors) {
        style?.let { SigunguOverlayLayer.apply(it, sigunguOverlay, mapColors) }
    }
    LaunchedEffect(style, sidoOverlay, mapColors) {
        style?.let { SidoOverlayLayer.apply(it, sidoOverlay, mapColors) }
    }
    // 0은 초기값(= 아직 요청 안 함) — activate()가 이미 최초 1회 추적을 돈다. 그 이후 값이 바뀔 때만
    // "내 위치" 버튼 요청으로 보고 다시 추적한다.
    LaunchedEffect(state.recenterRequestId, liveMap) {
        if (state.recenterRequestId == 0) return@LaunchedEffect
        liveMap?.let { CurrentLocationLayer.recenter(it) }
    }
    // 0은 초기값(= 아직 요청 안 함) — [MapState.focusOnBounds]가 호출될 때만 카메라를 옮긴다.
    LaunchedEffect(state.boundsFocusRequestId, liveMap) {
        if (state.boundsFocusRequestId == 0) return@LaunchedEffect
        val map = liveMap ?: return@LaunchedEffect
        val points = state.boundsFocusPoints ?: return@LaunchedEffect
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it.toMapLibreLatLng()) }
        val paddingPx = with(density) { BOUNDS_FOCUS_PADDING_DP.dp.toPx() }.roundToInt()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), paddingPx))
    }
}

private fun setUpInitialCamera(map: MapLibreMap, state: MapState) {
    map.uiSettings.isLogoEnabled = false
    map.uiSettings.isAttributionEnabled = false

    map.cameraPosition = CameraPosition.Builder()
        .target(state.initialCenter.toMapLibreLatLng())
        .zoom(state.initialZoom)
        .build()
    map.setLatLngBoundsForCameraTarget(
        LatLngBounds.from(
            MapConfig.KOREA_BOUNDS_NORTH_WEST.lat,
            MapConfig.KOREA_BOUNDS_SOUTH_EAST.lon,
            MapConfig.KOREA_BOUNDS_SOUTH_EAST.lat,
            MapConfig.KOREA_BOUNDS_NORTH_WEST.lon,
        ),
    )
    map.setMinZoomPreference(MapConfig.MIN_ZOOM_PREFERENCE)
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = mapViewLifecycleObserver(mapView)
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return mapView
}

private fun mapViewLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> Unit
        }
    }
