package com.swyp.runveal.devicestate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * 기기의 위치 서비스(GPS) 켜짐 여부. **앱 권한과는 별개다** — 권한을 허용했어도 기기에서 위치
 * 서비스가 꺼져 있으면 위치를 못 받는다. 권한 쪽은 `core:permission`의
 * `rememberLocationPermissionState`가 따로 다룬다.
 */
@Stable
class LocationServicesState internal constructor(
    val isEnabled: Boolean,
    private val onRequestEnable: () -> Unit,
) {
    /**
     * 위치 서비스를 켜 달라고 요청한다. 설정 앱으로 보내지 않고 Play Services의 "위치 켜기" 확인
     * 다이얼로그를 띄운다 — 그 자리에서 켜고 하던 일을 이어갈 수 있다. Play Services가 없는
     * 기기처럼 다이얼로그를 띄울 수 없으면 설정 화면으로 보낸다.
     */
    fun requestEnable() = onRequestEnable()
}

/**
 * 위치 서비스 켜짐 여부를 구독한다. 두 경로를 모두 본다.
 *  - `PROVIDERS_CHANGED` 브로드캐스트: 알림창 빠른 설정 타일로 끄고 켜면 화면을 안 떠나므로
 *    이걸로만 즉시 잡힌다.
 *  - `ON_RESUME`: 설정 앱에 다녀오는 경우.
 */
@Composable
fun rememberLocationServicesState(): LocationServicesState {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(context.isLocationServicesEnabled()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = context.isLocationServicesEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                enabled = context.isLocationServicesEnabled()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // 다이얼로그를 띄운 뒤 실제로 켜졌는지는 위 PROVIDERS_CHANGED 리시버가 갱신하므로 결과는 안 본다.
    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {}

    return remember(enabled, enableLauncher, context) {
        LocationServicesState(
            isEnabled = enabled,
            onRequestEnable = { context.requestEnableLocationServices(enableLauncher) },
        )
    }
}

private fun Context.isLocationServicesEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

private fun Context.requestEnableLocationServices(launcher: ActivityResultLauncher<IntentSenderRequest>) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).build()
    val settingsRequest = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
    LocationServices.getSettingsClient(this)
        .checkLocationSettings(settingsRequest)
        .addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    launcher.launch(IntentSenderRequest.Builder(exception.resolution).build())
                    return@addOnFailureListener
                } catch (_: IntentSender.SendIntentException) {
                    // 아래에서 설정 화면으로 보낸다.
                }
            }
            // Play Services가 없는 기기 등 다이얼로그로 해결할 수 없으면 설정 화면으로 보낸다.
            openLocationSettings()
        }
}

/** 기기 위치 서비스(GPS) 설정 화면을 연다. */
fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}
