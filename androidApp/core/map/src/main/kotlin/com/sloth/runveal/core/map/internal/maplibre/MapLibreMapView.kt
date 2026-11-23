package com.sloth.runveal.core.map.internal.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.sloth.runveal.core.map.DongOverlayGeoJson
import com.sloth.runveal.core.map.FollowCurrentLocationCommand
import com.sloth.runveal.core.map.MapState
import com.sloth.runveal.core.map.SidoOverlayGeoJson
import com.sloth.runveal.core.map.SigunguOverlayGeoJson
import com.sloth.runveal.core.map.internal.glow.DongGlowProvider
import com.sloth.runveal.designsystem.component.map.RunvealMapMarker
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerStyle
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.dong.DongPolygonPart
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.MapConfig
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
    dongBoundarySourceKey: String,
    sigunguOverlay: SigunguOverlayGeoJson?,
    sidoOverlay: SidoOverlayGeoJson?,
    locationPermissionGranted: Boolean,
    keepZoomedInStyle: Boolean,
) {
    val mapView = rememberMapViewWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    var style by remember { mutableStateOf<Style?>(null) }
    var liveMap by remember { mutableStateOf<MapLibreMap?>(null) }
    // 카메라가 멈출 때마다(OnCameraIdle) 화면에 보이는 동 빛 이미지를 다시 계산하기 위한 트리거.
    var cameraIdleTick by remember { mutableIntStateOf(0) }
    // 빛 이미지 생성이 끝날 때마다 올라가는 트리거. 생성은 비동기라 요청한 그 자리에선 아직 이미지가
    // 없고, 이게 없으면 캐시가 빈 첫 실행에서 지도를 한 번 움직이기 전까지 빛이 안 붙는다.
    var glowAssetReadyTick by remember { mutableIntStateOf(0) }
    val dongGlowProvider = remember { DongGlowProvider(context) { glowAssetReadyTick++ } }
    val dongGlowImageLayer = remember { DongGlowImageLayer(dongGlowProvider) }
    val baseStyleRoadFocus = remember { BaseStyleRoadFocus() }
    val baseMapDimmer = remember { BaseMapDimmer() }
    DisposableEffect(Unit) { onDispose { dongGlowProvider.dispose() } }
    val colors = RunvealTheme.colors
    val mapColors = remember(colors) {
        MapColors(
            visitedRoadGlow = colors.accent.glow.toArgb(),
            visitedRoadCore = colors.map.roadExplored.toArgb(),
            dongLabelPrimary = colors.text.primary.toArgb(),
            dongLabelSecondary = colors.text.secondary.toArgb(),
            sigunguLabel = colors.text.secondary.toArgb(),
            recommendedRouteAlt = colors.map.routeAlt.toArgb(),
            recommendedRouteSelectedGlow = colors.map.roadNewHalo.toArgb(),
            recommendedRouteSelected = colors.map.roadNew.toArgb(),
            courseGuide = colors.text.primary.toArgb(),
            liveRunOld = colors.map.roadToday.toArgb(),
            liveRunNew = colors.map.roadNew.toArgb(),
            liveRunNewGlow = colors.map.roadNewHalo.toArgb(),
            roadUnexploredFocus = colors.map.roadUnexploredFocus.toArgb(),
        )
    }

    // 선택 중 임시 핀([MapState.pin], Pin 스타일), 항상 떠 있는 점 마커([MapState.dotMarkers], Dot
    // 스타일), 항상 떠 있는 핀 마커([MapState.pinMarkers], Pin 스타일)를 하나의 목록으로 합쳐서 그린다
    // — 셋 다 결국 좌표 + 종류일 뿐이라 화면 좌표 계산은 같다.
    val renderedMarkers = state.dotMarkers.map { it to RunvealMapMarkerStyle.Dot } +
        state.pinMarkers.map { it to RunvealMapMarkerStyle.Pin } +
        (state.pin?.let { listOf(it to RunvealMapMarkerStyle.Pin) } ?: emptyList())
    // 카메라 이동 리스너는 스타일이 처음 로드될 때 딱 한 번만 등록돼서, 그 안의 람다가 그 시점의
    // renderedMarkers를 그대로 캡처해 버리면 이후 마커가 추가·변경돼도 리스너는 그 "첫 스냅샷"(보통
    // 마커가 하나도 없는 상태)으로 돌아가 버린다 — 지도를 움직일 때마다 마커가 사라지는 원인이었다.
    // rememberUpdatedState로 항상 최신 값을 참조하게 한다.
    val currentRenderedMarkers = rememberUpdatedState(renderedMarkers)
    var markerScreenOffsets by remember { mutableStateOf<List<Offset?>>(emptyList()) }
    fun refreshMarkerScreenOffsets() {
        val map = liveMap
        markerScreenOffsets = currentRenderedMarkers.value.map { (pin, _) ->
            map?.projection?.toScreenLocation(pin.location.toMapLibreLatLng())?.let { Offset(it.x, it.y) }
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
                    map.addOnCameraMoveListener { refreshMarkerScreenOffsets() }
                    // 사용자가 지도를 직접 움직이면(드래그·핀치) 현재 위치 추적을 푼다 — 따라가는
                    // 카메라와 손가락이 서로 지도를 잡아당기지 않게. 추적이 만드는 카메라 이동은
                    // REASON_DEVELOPER_ANIMATION/REASON_API_ANIMATION이라 여기서 걸리지 않는다.
                    map.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            state.stopFollowingCurrentLocation()
                        }
                    }
                    map.addOnMapClickListener { latLng ->
                        state.lastMapClick = LatLon(latLng.latitude, latLng.longitude)
                        true
                    }
                    map.setStyle(MapConfig.STYLE_URL) { loadedStyle ->
                        style = loadedStyle
                        // 베이스 스타일 레이어 스냅샷이 필요해서 다른 레이어를 추가하기 전에 가장 먼저
                        // 기억해 둔다 — 실제 딤·색 보정은 아래 LaunchedEffect가 화면에 맞춰 적용한다.
                        baseMapDimmer.rememberBaseLayers(loadedStyle)
                        // 도로 색을 바꾸기 전의 원래 색을 기억해 둬야 "안 가본 길" 토글을 끌 때 되돌릴 수 있다.
                        baseStyleRoadFocus.rememberOriginalColors(loadedStyle)
                        // 현재 위치 점은 여기서 켜지 않는다 — 이 블록은 스타일 로드 때 한 번만 도는데
                        // 그 시점엔 아직 위치 권한이 없는 게 정상이라(아래 LaunchedEffect 주석) 한 번만
                        // 시도하면 그 세션 내내 내 위치가 안 뜬다.
                        BaseStyleLabels.hide(loadedStyle)
                    }
                }
            }
        }

        LaunchedEffect(renderedMarkers, liveMap) { refreshMarkerScreenOffsets() }

        renderedMarkers.forEachIndexed { index, (pin, style) ->
            val screenOffset = markerScreenOffsets.getOrNull(index) ?: return@forEachIndexed
            key(pin, style) {
                when (style) {
                    RunvealMapMarkerStyle.Pin -> {
                        // Pin 디자인의 앵커(바닥 점의 중심)는 Column 맨 아래 8dp 점의 중앙이라 가로는
                        // 절반, 세로는 (전체 높이 - 점 반지름)만큼 끌어올려야 탭한 좌표와 점이 겹친다.
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
                    RunvealMapMarkerStyle.Dot -> {
                        // Dot는 원 하나라 좌표가 정가운데 — 가로·세로 모두 절반만큼 당긴다.
                        var dotSize by remember { mutableStateOf(IntSize.Zero) }
                        Box(
                            modifier = Modifier
                                .onSizeChanged { dotSize = it }
                                .offset {
                                    IntOffset(
                                        x = (screenOffset.x - dotSize.width / 2f).roundToInt(),
                                        y = (screenOffset.y - dotSize.height / 2f).roundToInt(),
                                    )
                                },
                        ) {
                            RunvealMapMarker(type = pin.type, style = RunvealMapMarkerStyle.Dot)
                        }
                    }
                }
            }
        }
    }

    // 베이스 지도 밝기·색 보정. 홈 지도에서는 줌에 따라 바뀌고(줌아웃=동 뷰는 흐리게), 홈이 아닌
    // 지도 화면(코스 추천·러닝 중)에서는 줌과 무관하게 "줌인 상태"로 고정한다 — 그 화면들은 동
    // 오버레이를 안 띄우니 흐리게 할 이유가 없고, 줌아웃하면 길이 안 보여 코스를 가늠할 수 없다.
    LaunchedEffect(style, keepZoomedInStyle) {
        val currentStyle = style ?: return@LaunchedEffect
        baseMapDimmer.apply(currentStyle, keepZoomedInStyle)
        BaseStyleDarkCorrection.apply(currentStyle, keepZoomedInStyle)
    }
    // "안 가본 길" 보기를 켠 동안만 베이스 지도 도로를 그 색으로 덮고, 끄면 원래 색으로 되돌린다.
    val unexploredRoadFocus = state.unexploredRoadFocus
    LaunchedEffect(style, unexploredRoadFocus, mapColors) {
        val focusColor = if (unexploredRoadFocus) mapColors.roadUnexploredFocus else null
        style?.let { baseStyleRoadFocus.apply(it, focusColor) }
    }
    val visitedRoadGeoJson = state.layers[MapConfig.SOURCE_VISITED_ROAD]
    LaunchedEffect(style, visitedRoadGeoJson, mapColors) {
        style?.let {
            VisitedRoadLayer.apply(it, MapConfig.SOURCE_VISITED_ROAD, visitedRoadGeoJson, mapColors.visitedRoadCore, mapColors.visitedRoadGlow)
        }
    }
    val liveRunOldGeoJson = state.layers[MapConfig.SOURCE_LIVE_RUN_OLD]
    LaunchedEffect(style, liveRunOldGeoJson, mapColors) {
        style?.let { VisitedRoadLayer.apply(it, MapConfig.SOURCE_LIVE_RUN_OLD, liveRunOldGeoJson, mapColors.liveRunOld) }
    }
    val liveRunNewGeoJson = state.layers[MapConfig.SOURCE_LIVE_RUN_NEW]
    LaunchedEffect(style, liveRunNewGeoJson, mapColors) {
        style?.let {
            VisitedRoadLayer.apply(it, MapConfig.SOURCE_LIVE_RUN_NEW, liveRunNewGeoJson, mapColors.liveRunNew, mapColors.liveRunNewGlow)
        }
    }
    val recommendedRouteGeoJson = state.layers[MapConfig.SOURCE_RECOMMENDED_ROUTE]
    LaunchedEffect(style, recommendedRouteGeoJson, mapColors) {
        style?.let { RecommendedRouteLayer.apply(it, MapConfig.SOURCE_RECOMMENDED_ROUTE, recommendedRouteGeoJson, mapColors) }
    }
    val courseDetailRouteGeoJson = state.layers[MapConfig.SOURCE_COURSE_DETAIL_ROUTE]
    LaunchedEffect(style, courseDetailRouteGeoJson, mapColors) {
        style?.let { RecommendedRouteLayer.apply(it, MapConfig.SOURCE_COURSE_DETAIL_ROUTE, courseDetailRouteGeoJson, mapColors) }
    }
    val courseGuideGeoJson = state.layers[MapConfig.SOURCE_RECORDING_COURSE_GUIDE]
    LaunchedEffect(style, courseGuideGeoJson, mapColors) {
        style?.let { CourseGuideLayer.apply(it, MapConfig.SOURCE_RECORDING_COURSE_GUIDE, courseGuideGeoJson, mapColors) }
    }
    LaunchedEffect(style, dongOverlay, mapColors) {
        style?.let { DongOverlayLayer.apply(it, dongOverlay, mapColors) }
    }
    // 동 빛 이미지. dongOverlay(단계 변경)·카메라 멈춤(화면 범위 변경)·생성 완료 중 하나라도 바뀌면
    // 다시 계산한다(docs/DONG_GLOW_ON_DEVICE.md Step 4).
    LaunchedEffect(style, liveMap, dongOverlay, dongGlowPolygonParts, dongBoundarySourceKey, cameraIdleTick, glowAssetReadyTick) {
        val currentStyle = style ?: return@LaunchedEffect
        val currentMap = liveMap ?: return@LaunchedEffect
        dongGlowImageLayer.refresh(
            currentStyle,
            currentMap,
            dongOverlay?.fillGeoJson,
            dongGlowPolygonParts,
            dongBoundarySourceKey,
        )
    }
    LaunchedEffect(style, sigunguOverlay, mapColors) {
        style?.let { SigunguOverlayLayer.apply(it, sigunguOverlay, mapColors) }
    }
    LaunchedEffect(style, sidoOverlay, mapColors) {
        style?.let { SidoOverlayLayer.apply(it, sidoOverlay, mapColors) }
    }
    // 현재 위치 점. 지도는 권한을 받기 **전에** 떠 있다 — `RequireLocationPermission`이 지도를 먼저
    // 그리고 그 위에 권한 안내 시트를 띄우기 때문이다. 그래서 스타일 로드 시점에 한 번만 켜려고 하면
    // 그때는 권한이 없어 조용히 실패하고, 사용자가 허용한 뒤에도 다시 켜 주는 경로가 없어서 앱을
    // 재시작할 때까지 내 위치가 안 뜬다(`isLocationComponentActivated`를 보는 "내 위치" 버튼도 같이
    // 죽는다). 권한 상태를 키로 둬서 허용되는 순간 켜 준다.
    LaunchedEffect(style, liveMap, locationPermissionGranted, mapColors) {
        if (!locationPermissionGranted) return@LaunchedEffect
        val currentStyle = style ?: return@LaunchedEffect
        val currentMap = liveMap ?: return@LaunchedEffect
        CurrentLocationLayer.activate(context, currentMap, currentStyle)
    }

    // 바라보는 방향 화살표(러닝 중). 위치 컴포넌트가 켜진 **뒤에** 걸어야 해서 위 activate와 같은
    // 조건(권한·스타일)을 키에 둔다 — 켜지는 순간 다시 적용된다.
    val showsHeadingIndicator = state.showsHeadingIndicator
    LaunchedEffect(showsHeadingIndicator, liveMap, style, locationPermissionGranted) {
        val map = liveMap ?: return@LaunchedEffect
        CurrentLocationLayer.setHeadingIndicatorVisible(map, showsHeadingIndicator)
    }
    // 현재 위치 추적(러닝 중) — 마지막 명령을 그대로 반영한다. 위치 컴포넌트가 아직 안 켜져 있으면
    // 추적 요청이 조용히 무시되므로, 그걸 켜는 조건(권한·스타일)도 키에 둔다 — 러닝 시작이 스타일
    // 로드보다 빨라도 켜지는 순간 다시 적용된다(위 activate 블록이 먼저 돈다).
    val followCommand = state.followCommand
    val followBottomInset = state.followBottomInset
    LaunchedEffect(followCommand, liveMap, style, locationPermissionGranted) {
        val map = liveMap ?: return@LaunchedEffect
        when (followCommand) {
            null -> Unit
            is FollowCurrentLocationCommand.Stop ->
                CurrentLocationLayer.stopFollowing(map, resetAnchor = followCommand.resetAnchor)
            is FollowCurrentLocationCommand.Start ->
                CurrentLocationLayer.startFollowing(
                    map = map,
                    zoom = followCommand.zoom,
                    // 호출 시점이 아니라 쓰는 시점의 값을 읽는다 — 러닝 시작 직후엔 패널 높이를 아직
                    // 재기 전이라 0이고, 추적 전환이 끝날 즈음엔 측정이 끝나 있다.
                    bottomPaddingPx = { with(density) { state.followBottomInset.toPx() }.toDouble() },
                )
        }
    }
    // 가리는 패널 높이가 바뀌면 여백만 다시 맞춘다 — 추적 중일 때만. 줌은 다시 건드리지 않는다
    // (사용자가 조절해 둔 배율이 패널 높이 변화에 되돌아가면 안 된다).
    LaunchedEffect(followBottomInset, followCommand, liveMap) {
        if (followCommand !is FollowCurrentLocationCommand.Start) return@LaunchedEffect
        val map = liveMap ?: return@LaunchedEffect
        CurrentLocationLayer.applyFollowBottomPadding(map, with(density) { followBottomInset.toPx() }.toDouble())
    }
    // 0은 초기값(= 아직 요청 안 함) — 위 activate()가 이미 최초 1회 추적을 돈다. 그 이후 값이 바뀔 때만
    // "내 위치" 버튼 요청으로 보고 다시 추적한다.
    LaunchedEffect(state.recenterRequestId, liveMap) {
        if (state.recenterRequestId == 0) return@LaunchedEffect
        liveMap?.let { CurrentLocationLayer.recenter(it, state.recenterZoom) }
    }
    // 0은 초기값(= 아직 요청 안 함) — [MapState.focusOnBounds]가 호출될 때만 카메라를 옮긴다.
    LaunchedEffect(state.boundsFocusRequestId, liveMap) {
        if (state.boundsFocusRequestId == 0) return@LaunchedEffect
        val map = liveMap ?: return@LaunchedEffect
        val points = state.boundsFocusPoints ?: return@LaunchedEffect
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it.toMapLibreLatLng()) }
        val paddingPx = with(density) { BOUNDS_FOCUS_PADDING_DP.dp.toPx() }.roundToInt()
        val extraBottomPx = with(density) { state.boundsFocusExtraBottomPadding.toPx() }.roundToInt()
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), paddingPx, paddingPx, paddingPx, paddingPx + extraBottomPx),
        )
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
