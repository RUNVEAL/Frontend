package com.swyp.runveal.core.map.internal.maplibre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.swyp.runveal.core.map.MapState
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.map.MapConfig
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
internal fun MapLibreMapView(
    modifier: Modifier,
    state: MapState,
) {
    val mapView = rememberMapViewWithLifecycle()
    var style by remember { mutableStateOf<Style?>(null) }
    val colors = RunvealTheme.colors
    val mapColors = remember(colors) {
        MapColors(
            visitedRoadGlow = colors.accent.glow.toArgb(),
            visitedRoadCore = colors.map.roadExplored.toArgb(),
            currentLocation = colors.map.location.toArgb(),
            dongLabelPrimary = colors.text.primary.toArgb(),
            dongLabelSecondary = colors.text.secondary.toArgb(),
        )
    }

    AndroidView(modifier = modifier, factory = { mapView }) { view ->
        view.getMapAsync { map ->
            if (style == null) {
                setUpInitialCamera(map, state)
                map.addOnCameraIdleListener { state.currentZoom = map.cameraPosition.zoom }
                map.setStyle(MapConfig.STYLE_URL) { loadedStyle ->
                    style = loadedStyle
                    CurrentLocationLayer.activate(view.context, map, loadedStyle, mapColors.currentLocation)
                }
            }
        }
    }

    val visitedRoadGeoJson = state.layers[MapConfig.SOURCE_VISITED_ROAD]
    LaunchedEffect(style, visitedRoadGeoJson, mapColors) {
        style?.let { VisitedRoadLayer.apply(it, visitedRoadGeoJson, mapColors) }
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
