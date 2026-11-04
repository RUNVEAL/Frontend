package com.swyp.runveal.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 위치 권한 요청 흐름 전체(자동 요청 + 허용 전 안내 UI + 허용 후 [content] 표시)를 여기서 끝낸다.
 * feature는 이 composable로 감싸기만 하면 되고, 권한 상태나 요청 로직을 직접 다루지 않는다.
 */
@Composable
fun RequireLocationPermission(
    modifier: Modifier = Modifier,
    rationale: @Composable (onRequest: () -> Unit) -> Unit = { onRequest -> DefaultLocationPermissionRationale(modifier, onRequest) },
    content: @Composable () -> Unit,
) {
    val state = rememberLocationPermissionState()

    LaunchedEffect(state.isGranted) {
        if (!state.isGranted) state.request()
    }

    if (state.isGranted) {
        content()
    } else {
        rationale(state::request)
    }
}

@Composable
private fun DefaultLocationPermissionRationale(modifier: Modifier = Modifier, onRequest: () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text("지도에 현재 위치를 표시하려면 위치 권한이 필요해요.")
            Button(onClick = onRequest) {
                Text("위치 권한 허용")
            }
        }
    }
}
