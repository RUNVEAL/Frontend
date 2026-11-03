package com.swyp.runveal.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * 위치 권한(FINE+COARSE) 요청 상태. 지도에 현재 위치 점을 띄우는 데 필요한 최소 권한만 다룬다 —
 * 알림/백그라운드 위치 같은 GPS 기록용 권한은 `core:tracking` 몫이라 여기 포함하지 않는다.
 */
@Stable
class LocationPermissionState internal constructor(
    val isGranted: Boolean,
    private val launcher: ActivityResultLauncher<Array<String>>,
) {
    fun request() = launcher.launch(LOCATION_PERMISSIONS)
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result.values.any { it }
    }
    return remember(granted, launcher) { LocationPermissionState(granted, launcher) }
}

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
