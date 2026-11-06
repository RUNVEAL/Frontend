package com.swyp.runveal.core.map.internal.maplibre

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnLocationCameraTransitionListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * 현재 위치 점 — 위치 권한은 feature(`core:permission`)에서 먼저 받고 나서 지도를 띄우는 걸 전제한다.
 * 권한이 있고 위치 서비스가 켜져 있어 실제 위치를 받으면(= 켜져 있지 않으면 업데이트 자체가 없어 아무
 * 일도 안 일어난다) 지도 카메라를 그 위치로 한 번 이동시키고, 이후에는 사용자가 자유롭게 둘러볼 수
 * 있도록 추적 모드를 끈다(계속 따라다니지 않음).
 */
internal object CurrentLocationLayer {
    /** 바로 위에서 수동으로 권한을 확인하고 없으면 return하는데, lint가 이 가드를 추론하지 못해 억제한다. */
    @SuppressLint("MissingPermission")
    fun activate(context: Context, map: MapLibreMap, style: Style, tintColor: Int) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val options = LocationComponentOptions.builder(context)
            .foregroundTintColor(tintColor)
            .build()
        val activationOptions = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(options)
            .build()

        val locationComponent = map.locationComponent
        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.setCameraMode(
            CameraMode.TRACKING,
            object : OnLocationCameraTransitionListener {
                override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                    locationComponent.cameraMode = CameraMode.NONE
                }

                override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
                    locationComponent.cameraMode = CameraMode.NONE
                }
            },
        )
    }
}
