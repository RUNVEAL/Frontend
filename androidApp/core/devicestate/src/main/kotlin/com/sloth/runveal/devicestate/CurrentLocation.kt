package com.sloth.runveal.devicestate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sloth.runveal.map.LatLon
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 지금 위치를 한 번 받아온다. 권한이 없거나(둘 다 미허용) 위치를 못 받으면 null — 호출부가 그
 * 경우를 각자 처리한다.
 *
 * 구독이 아니라 **일회성 조회**다. 계속 따라다녀야 하는 화면은 `core:tracking`의 위치 수신을 쓴다.
 */
@SuppressLint("MissingPermission") // 바로 아래에서 권한을 확인하고 없으면 return한다.
suspend fun Context.currentLocationOrNull(): LatLon? {
    val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return null

    val client = LocationServices.getFusedLocationProviderClient(this)
    val cancellationTokenSource = CancellationTokenSource()
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                continuation.resume(location?.let { LatLon(lat = it.latitude, lon = it.longitude) })
            }
            .addOnFailureListener { continuation.resume(null) }
    }
}
