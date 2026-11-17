package com.swyp.runveal.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import com.swyp.runveal.external.ImportUnavailableReason
import com.swyp.runveal.health.HealthConnectAvailability
import com.swyp.runveal.health.HealthConnectPermissions
import kotlinx.coroutines.launch

/**
 * Health Connect로 넘어가기 전에 권한을 확인하는 게이트. 돌려주는 람다를 버튼의 `onClick`에 그대로
 * 걸면 **누른 그 자리에서** 권한을 보고 갈라진다.
 *  - 권한이 다 있으면 바로 [onGranted].
 *  - 모자라면 Health Connect 권한 화면을 띄우고, 허용하고 돌아왔을 때 [onGranted]. 그래도 모자라면
 *    [onDenied].
 *  - 기기에 Health Connect가 없거나 업데이트가 필요하면 [onUnavailable] — 권한 화면조차 못 띄운다.
 *
 * Health Connect로 가는 **모든 입구**(마이페이지의 "기록 불러오기", 안내 화면의 불러오기 버튼)에
 * 같은 게이트를 걸어 둔다. 한 곳만 확인하면 나머지 입구로 들어온 사용자는 권한 없이 다음 화면까지
 * 갔다가 거기서 막힌다.
 *
 * 화면에 들어올 때 미리 조회하지 않는 건 의도다 — 권한 조회가 비동기라, 미리 재 두면 "아직 확인
 * 중"인 짧은 구간이 생기고 그 사이 누른 탭을 따로 붙잡아 둬야 한다. 누를 때 묻는 쪽이 단순하고,
 * 사용자가 Health Connect 앱에서 권한을 바꾸고 돌아온 경우에도 항상 최신 값을 본다.
 *
 * 위치 권한([rememberLocationPermissionState])처럼 영구 거부를 따로 보지 않는다: Health Connect는
 * 거부 횟수로 요청을 막지 않고, 사용자가 언제든 Health Connect 앱에서 다시 켤 수 있다.
 */
@Composable
fun rememberHealthConnectPermissionGate(
    onGranted: () -> Unit,
    onUnavailable: () -> Unit = {},
    onDenied: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    // 화면보다 오래 사는 Health Connect 클라이언트를 쥐므로 application context를 준다.
    val availability = remember(context) { HealthConnectAvailability(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        // 돌아온 결과를 직접 보지 않고 다시 묻는다 — 필요한 권한이 세션·경로 두 묶음이고, 무엇이
        // 모자라면 못 쓰는지는 HealthConnectAvailability가 이미 알고 있다.
        scope.launch {
            when (availability.unavailableReason()) {
                null -> onGranted()
                ImportUnavailableReason.PROVIDER_NOT_AVAILABLE -> onUnavailable()
                ImportUnavailableReason.PERMISSION_DENIED,
                ImportUnavailableReason.ROUTE_PERMISSION_DENIED,
                -> onDenied()
            }
        }
    }

    return {
        scope.launch {
            when (availability.unavailableReason()) {
                null -> onGranted()
                ImportUnavailableReason.PROVIDER_NOT_AVAILABLE -> onUnavailable()
                ImportUnavailableReason.PERMISSION_DENIED,
                ImportUnavailableReason.ROUTE_PERMISSION_DENIED,
                -> launcher.launch(HealthConnectPermissions.all)
            }
        }
        Unit
    }
}
