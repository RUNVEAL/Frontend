package com.swyp.runveal.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.swyp.runveal.designsystem.component.overlay.RunvealLocationPermissionSettingsSheet
import com.swyp.runveal.designsystem.component.overlay.RunvealLocationPermissionSheet

/**
 * 위치 권한 요청 흐름 전체를 여기서 끝낸다. [content]는 권한 유무와 상관없이 항상 그리고,
 * 미승인 상태면 그 위에 안내 바텀시트를 띄운다. 시스템 권한 다이얼로그는 진입 시 자동으로
 * 띄우지 않고, 바텀시트의 '위치 권한 허용하기' 버튼을 눌렀을 때만 띄운다. 2회 이상 거부해
 * 시스템 다이얼로그를 더 띄울 수 없는 상태(영구 거부)면 설정 화면으로 보내는 바텀시트를 대신 띄운다.
 * feature는 이 composable로 감싸기만 하면 되고, 권한 상태나 요청 로직을 직접 다루지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequireLocationPermission(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberLocationPermissionState()
    var dismissed by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        content()
    }

    if (!state.isGranted && !dismissed) {
        ModalBottomSheet(
            onDismissRequest = { dismissed = true },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            dragHandle = null,
            // 기본 inset을 끄고 시트 배경(RunvealBottomSheet)이 OS 내비게이션 바 뒤까지 그대로 채우게 한다.
            // 버튼만 내부에서 그 inset만큼 띄운다(RunvealLocationPermissionSheet 쪽에서 처리).
            contentWindowInsets = { WindowInsets(0) },
        ) {
            if (state.isPermanentlyDenied) {
                RunvealLocationPermissionSettingsSheet(
                    onSettingsClick = { context.openAppSettings() },
                    onCloseClick = { dismissed = true },
                )
            } else {
                RunvealLocationPermissionSheet(
                    onAllowClick = state::request,
                    onLaterClick = { dismissed = true },
                )
            }
        }
    }
}

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}
