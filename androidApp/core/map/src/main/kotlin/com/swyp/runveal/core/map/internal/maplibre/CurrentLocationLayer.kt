package com.swyp.runveal.core.map.internal.maplibre

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnLocationCameraTransitionListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * 현재 위치 점. 권한이 있고 위치 서비스가 켜져 있어 실제 위치를 받으면(= 켜져 있지 않으면 업데이트
 * 자체가 없어 아무 일도 안 일어난다) 지도 카메라를 그 위치로 한 번 이동시키고, 이후에는 사용자가
 * 자유롭게 둘러볼 수 있도록 추적 모드를 끈다(계속 따라다니지 않음).
 *
 * 지도는 권한을 받기 **전에** 떠 있다 — `RequireLocationPermission`이 지도를 먼저 그리고 그 위에
 * 권한 안내 시트를 띄우기 때문이다. 그래서 [activate]는 권한을 늦게 허용한 뒤에 다시 불린다
 * (`MapLibreMapView`가 권한 상태를 키로 재시도). 두 번째 호출부터는 이미 활성화된 걸 보고 바로
 * 돌아가므로 중복 활성화가 일어나지 않는다.
 */
internal object CurrentLocationLayer {
    /** 바로 위에서 수동으로 권한을 확인하고 없으면 return하는데, lint가 이 가드를 추론하지 못해 억제한다. */
    @SuppressLint("MissingPermission")
    fun activate(context: Context, map: MapLibreMap, style: Style, tintColor: Int) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        // 권한을 늦게 허용하면 다시 불리는 함수다 — 이미 켜 뒀으면 아무것도 하지 않는다.
        if (map.locationComponent.isLocationComponentActivated) return

        val options = LocationComponentOptions.builder(context)
            .foregroundTintColor(tintColor)
            .build()
        val activationOptions = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(options)
            .build()

        val locationComponent = map.locationComponent
        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        trackOnce(locationComponent)
    }

    /** "내 위치" 버튼 — [activate]로 이미 켜져 있는 컴포넌트를 재사용해 같은 one-shot 추적을 다시 돌린다.
     * 권한이 없거나 위치 컴포넌트가 아직 활성화되지 않았으면([activate]가 조용히 return한 경우) 아무
     * 일도 하지 않는다. */
    fun recenter(map: MapLibreMap) {
        if (!map.locationComponent.isLocationComponentActivated) return
        trackOnce(map.locationComponent)
    }

    private fun trackOnce(locationComponent: LocationComponent) {
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
