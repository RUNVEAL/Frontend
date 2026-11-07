package com.swyp.runveal.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private const val PREFS_NAME = "runveal_location_permission"
private const val KEY_REQUESTED_BEFORE = "requested_before"

/**
 * 위치 권한(FINE+COARSE) 요청 상태. 지도에 현재 위치 점을 띄우는 데 필요한 최소 권한만 다룬다 —
 * 알림/백그라운드 위치 같은 GPS 기록용 권한은 `core:tracking` 몫이라 여기 포함하지 않는다.
 *
 * [isPermanentlyDenied]는 사용자가 권한을 2회 이상(또는 '다시 묻지 않음') 거부해 시스템이 더 이상
 * 권한 다이얼로그를 띄워주지 않는 상태. `shouldShowRequestPermissionRationale`이 false면 "한 번도
 * 요청 안 함"과 "영구 거부"를 구분할 수 없어서, 최초 요청 여부를 SharedPreferences에 따로 남겨
 * 앱을 재시작해도(= 이 Composable이 처음 호출되는 시점에도) 바로 영구 거부 여부를 판별한다.
 */
@Stable
class LocationPermissionState internal constructor(
    val isGranted: Boolean,
    val isPermanentlyDenied: Boolean,
    private val onRequest: () -> Unit,
) {
    fun request() = onRequest()
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    var permanentlyDenied by remember { mutableStateOf(context.isPermanentlyDenied(activity, granted)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result.values.any { it }
        permanentlyDenied = context.isPermanentlyDenied(activity, granted)
    }

    // 시스템 설정 앱에서 권한을 켜고(혹은 끄고) 돌아오는 경우는 launcher 콜백을 안 거치므로, 화면이 다시
    // 보일 때마다(ON_RESUME) 권한 상태를 다시 읽어서 반영한다 — 그래야 설정에서 허용하고 돌아오면
    // 바텀시트가 사라진다.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = context.hasLocationPermission()
                permanentlyDenied = context.isPermanentlyDenied(activity, granted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(granted, permanentlyDenied, launcher) {
        LocationPermissionState(
            isGranted = granted,
            isPermanentlyDenied = permanentlyDenied,
            onRequest = {
                context.markLocationPermissionRequestedBefore()
                launcher.launch(LOCATION_PERMISSIONS)
            },
        )
    }
}

/**
 * `shouldShowRequestPermissionRationale`은 "한 번도 요청 안 함"과 "영구 거부" 둘 다 false라 혼자서는
 * 구분이 안 된다 — 반드시 [hasRequestedLocationPermissionBefore]와 함께 써야 영구 거부만 걸러진다.
 */
private fun Context.isPermanentlyDenied(activity: Activity, granted: Boolean): Boolean =
    !granted &&
        hasRequestedLocationPermissionBefore() &&
        LOCATION_PERMISSIONS.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

private fun Context.hasRequestedLocationPermissionBefore(): Boolean =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_REQUESTED_BEFORE, false)

private fun Context.markLocationPermissionRequestedBefore() {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REQUESTED_BEFORE, true)
        .apply()
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("위치 권한 요청에는 Activity Context가 필요합니다.")
}
