package com.sloth.runveal.ui.network

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sloth.runveal.devicestate.NetworkState
import com.sloth.runveal.devicestate.rememberNetworkState

/**
 * 어느 화면에 있든 네트워크가 끊기면 그 위를 [NetworkErrorScreen]으로 덮는다. 화면마다 따로
 * 붙이지 않고 앱 최상위(`App.kt`)에 한 번만 두면 된다.
 *
 * 닫을 수 있다 — 뒤로가기(시스템 back 또는 상단 앱바 화살표)를 누르면 원래 화면으로 돌아가고,
 * 연결이 없어도 그대로 앱을 쓸 수 있다. 네트워크가 필요 없는 일(지난 기록 보기, 러닝 기록 등)을
 * 막지 않기 위해서다. 닫은 뒤에는 **연결이 돌아왔다가 다시 끊길 때** 다시 뜬다 — 끊긴 상태가
 * 계속되는 동안 매번 다시 덮으면 아무것도 할 수 없다.
 *
 * 연결이 돌아오면 저절로 내려간다(`NetworkCallback` 구독, [rememberNetworkState]).
 */
@Composable
fun NetworkErrorOverlay(
    modifier: Modifier = Modifier,
    state: NetworkState = rememberNetworkState(),
) {
    var dismissed by remember { mutableStateOf(false) }

    // 연결이 돌아오면 "닫음" 표시를 푼다 — 다음에 또 끊기면 다시 알려야 한다.
    LaunchedEffect(state.isConnected) {
        if (state.isConnected) dismissed = false
    }

    if (state.isConnected || dismissed) return

    // 오버레이가 떠 있는 동안만 뒤로가기를 가로챈다 — 이 블록 안에서만 등록되므로, 닫힌 뒤에는
    // 뒤로가기가 평소대로 화면 이동에 쓰인다.
    BackHandler { dismissed = true }

    NetworkErrorScreen(
        // 연결이 돌아왔는지 다시 읽는다. 연결됐으면 isConnected가 true로 바뀌어 이 화면이 내려간다
        // — 버튼이 직접 닫지 않는다.
        onRetryClick = state::refresh,
        onBackClick = { dismissed = true },
        modifier = modifier,
    )
}
