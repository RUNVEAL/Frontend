package com.sloth.runveal.core.map.internal.maplibre

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.maplibre.android.camera.CameraUpdateFactory
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
 * 러닝 중처럼 계속 따라가야 할 때만 [startFollowing]으로 추적을 켜고, [stopFollowing]으로 끈다.
 *
 * 지도는 권한을 받기 **전에** 떠 있다 — `RequireLocationPermission`이 지도를 먼저 그리고 그 위에
 * 권한 안내 시트를 띄우기 때문이다. 그래서 [activate]는 권한을 늦게 허용한 뒤에 다시 불린다
 * (`MapLibreMapView`가 권한 상태를 키로 재시도). 두 번째 호출부터는 이미 활성화된 걸 보고 바로
 * 돌아가므로 중복 활성화가 일어나지 않는다.
 */
internal object CurrentLocationLayer {
    /** 추적을 켤 때 카메라가 현재 위치로 옮겨 가는 시간 — MapLibre 기본값과 같다. */
    private const val FOLLOW_TRANSITION_DURATION_MS = 750L

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

    /**
     * 현재 위치를 계속 따라가는 추적을 켠다(러닝 중). 방위는 북쪽으로 고정한다
     * ([CameraMode.TRACKING_GPS_NORTH]) — 달리는 방향으로 지도가 돌면 길을 알아보기 어렵다.
     *
     * [zoom]을 주면 그 줌으로 맞추고, null이면 지금 줌을 그대로 둔다("내 위치" 버튼·러닝 재개).
     * [bottomPaddingPx]는 지도 아래쪽이 패널에 가려지는 높이 — 그만큼 카메라 아래 여백을 줘서 현재
     * 위치가 "보이는 영역의 가운데"에 오게 한다(여백을 뺀 영역의 가운데가 카메라 중심이다). 호출
     * 시점이 아니라 **쓰는 시점**의 높이를 읽어야 해서 람다로 받는다(아래 전환 뒤 재적용 참고).
     *
     * 줌·여백은 추적 모드에서만 반영되므로([LocationComponent.zoomWhileTracking] 등 모드가
     * [CameraMode.NONE]이면 조용히 무시) 모드를 바꾼 **뒤에** 준다.
     *
     * 추적이 꺼져 있다가 켜지는 경우(러닝 시작)엔 SDK가 현재 위치로 카메라를 옮기는 전환
     * 애니메이션을 돌리는데, 그동안 들어온 여백은 그냥 버린다(`LocationCameraController.setPadding`의
     * 첫 줄이 `if (isTransitioning) return`). 그래서 **전환이 끝난 뒤 한 번 더** 건다 — 그 시점엔
     * 이미 추적 중이라, 러닝 중에 "내 위치"를 누른 것과 똑같은 상태로 끝난다. 이미 추적 중이면
     * (= "내 위치" 버튼) 전환 자체가 없어 지금까지와 동작이 같다.
     */
    fun startFollowing(map: MapLibreMap, zoom: Double?, bottomPaddingPx: () -> Double) {
        val locationComponent = map.locationComponent
        if (!locationComponent.isLocationComponentActivated) return

        val needsTransition = locationComponent.cameraMode != CameraMode.TRACKING_GPS_NORTH
        locationComponent.setCameraMode(
            CameraMode.TRACKING_GPS_NORTH,
            FOLLOW_TRANSITION_DURATION_MS,
            zoom,
            null, // bearing — 북쪽 고정은 CameraMode가 맡는다.
            null, // tilt — 기울이지 않는다.
            if (needsTransition) {
                object : OnLocationCameraTransitionListener {
                    override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                        applyFollowBottomPadding(map, bottomPaddingPx())
                    }

                    override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
                        applyFollowBottomPadding(map, bottomPaddingPx())
                    }
                }
            } else {
                null
            },
        )
        applyFollowBottomPadding(map, bottomPaddingPx())
    }

    /** 추적 중 아래 여백만 다시 맞춘다 — 가리는 패널 높이가 바뀔 때. 추적 중이 아니면 무시된다. */
    fun applyFollowBottomPadding(map: MapLibreMap, bottomPaddingPx: Double) {
        val locationComponent = map.locationComponent
        if (!locationComponent.isLocationComponentActivated) return
        locationComponent.paddingWhileTracking(doubleArrayOf(0.0, 0.0, 0.0, bottomPaddingPx))
    }

    /**
     * 추적을 끈다(러닝 일시정지·화면 이탈·지도 제스처).
     *
     * [resetAnchor]면 [startFollowing]이 준 카메라 여백까지 0으로 되돌린다 — 러닝 화면을 나갈 때처럼
     * 지도를 다른 화면에 돌려줄 때만. 여백을 되돌리면 같은 좌표가 화면에서 그만큼 위로 올라오므로
     * (= 지도가 튄다) 일시정지·드래그로 끌 때는 그대로 둔다. 여백은 추적 모드에서만 바꿀 수 있어서
     * ([LocationComponent.paddingWhileTracking]) 지도 카메라 쪽으로 직접 되돌린다.
     */
    fun stopFollowing(map: MapLibreMap, resetAnchor: Boolean) {
        val locationComponent = map.locationComponent
        if (!locationComponent.isLocationComponentActivated) return
        locationComponent.cameraMode = CameraMode.NONE
        if (resetAnchor) map.moveCamera(CameraUpdateFactory.paddingTo(0.0, 0.0, 0.0, 0.0))
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
