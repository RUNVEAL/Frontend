package com.sloth.runveal.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

private const val PREFS_NAME = "runveal_notification_permission"
private const val KEY_REQUESTED_BEFORE = "requested_before"

/**
 * 알림 권한(`POST_NOTIFICATIONS`) 요청 상태. Android 13(API 33)부터 런타임 권한이라 매니페스트
 * 선언만으로는 알림이 뜨지 않는다 — 13 미만에서는 권한 자체가 없어 항상 [isGranted]다.
 *
 * **이 권한이 없어도 포그라운드 서비스는 정상으로 돈다.** `TrackingService`는 `startForeground()`에
 * 넘길 Notification 객체만 있으면 승격되고 위치 수집도 계속된다 — 권한은 그 알림을 *사용자에게
 * 보여줄* 때만 관여한다. 그래서 거부돼도 러닝을 막지 않는다([RequestNotificationPermissionOnce]).
 *
 * 그래도 받아야 하는 이유는 둘이다.
 *  1. 앱을 내린 뒤 러닝 중인지 확인할 방법이 알림뿐이다.
 *  2. 알림의 "종료" 액션으로 러닝을 끝낼 수 있다 — 없으면 앱을 다시 열어야 한다.
 *
 * [isPermanentlyDenied]는 위치 권한([rememberLocationPermissionState])과 같은 방식으로 판별한다 —
 * `shouldShowRequestPermissionRationale`만으로는 "한 번도 요청 안 함"과 "영구 거부"가 구분되지
 * 않아서, 최초 요청 여부를 SharedPreferences에 따로 남긴다. 영구 거부는 앱에서 되돌릴 수 없어
 * [Context.openAppSettings]로 보내는 수밖에 없다.
 */
@Stable
class NotificationPermissionState internal constructor(
    val isGranted: Boolean,
    val isPermanentlyDenied: Boolean,
    private val onRequest: () -> Unit,
) {
    fun request() = onRequest()
}

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var granted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var permanentlyDenied by remember { mutableStateOf(context.isNotificationPermanentlyDenied(activity, granted)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        permanentlyDenied = context.isNotificationPermanentlyDenied(activity, granted)
    }

    // 설정 앱에서 알림을 켜고(혹은 끄고) 돌아오는 경우는 launcher 콜백을 안 거치므로, 화면이 다시
    // 보일 때마다 다시 읽는다 — 위치 권한과 같은 이유다.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = context.hasNotificationPermission()
                permanentlyDenied = context.isNotificationPermanentlyDenied(activity, granted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(granted, permanentlyDenied, launcher) {
        NotificationPermissionState(
            isGranted = granted,
            isPermanentlyDenied = permanentlyDenied,
            onRequest = {
                if (!granted && NEEDS_RUNTIME_PERMISSION) {
                    context.markNotificationPermissionRequestedBefore()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}

/**
 * 들어온 자리에서 **한 번만** 알림 권한을 묻는다. 결과를 기다리지 않고 아무것도 막지 않는다 —
 * 호출하는 화면은 권한이 있든 없든 그대로 진행한다.
 *
 * 러닝 시작(`RecordingScreen`)처럼 "알림이 있으면 좋지만 없어도 기능이 성립하는" 자리에 쓴다.
 * 앱 첫 실행에 몰아서 묻지 않는 건 의도다 — 왜 필요한지 맥락이 없는 상태에서 물으면 거부율이
 * 높고, 한 번 거부되면 시스템이 다이얼로그를 다시 띄워주지 않는다.
 *
 * 이미 허용했거나 영구 거부 상태면 아무 일도 하지 않는다. 영구 거부에 요청을 또 넣어도 다이얼로그
 * 없이 즉시 거부로 돌아오기만 해서 의미가 없다.
 */
@Composable
fun RequestNotificationPermissionOnce(
    state: NotificationPermissionState = rememberNotificationPermissionState(),
) {
    LaunchedEffect(Unit) {
        if (!state.isGranted && !state.isPermanentlyDenied) state.request()
    }
}

/** Android 13 미만에는 `POST_NOTIFICATIONS`가 없다 — 선언도 요청도 필요 없고 알림이 그냥 뜬다. */
private val NEEDS_RUNTIME_PERMISSION: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private fun Context.hasNotificationPermission(): Boolean =
    !NEEDS_RUNTIME_PERMISSION ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.isNotificationPermanentlyDenied(activity: Activity, granted: Boolean): Boolean =
    !granted &&
        hasRequestedNotificationPermissionBefore() &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)

private fun Context.hasRequestedNotificationPermissionBefore(): Boolean =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_REQUESTED_BEFORE, false)

private fun Context.markNotificationPermissionRequestedBefore() {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REQUESTED_BEFORE, true)
        .apply()
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("알림 권한 요청에는 Activity Context가 필요합니다.")
}
