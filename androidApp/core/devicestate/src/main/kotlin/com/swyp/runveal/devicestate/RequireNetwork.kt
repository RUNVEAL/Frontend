package com.swyp.runveal.devicestate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 네트워크가 있어야만 쓸 수 있는 화면을 감싼다. 연결이 없으면 [content] 대신
 * [NetworkErrorScreen]을 보여주고, 연결되는 순간 [content]로 돌아간다 — 와이파이를 켜거나
 * "다시 시도"를 누르면 그 자리에서 바뀐다.
 *
 * 위치 권한 흐름([com.swyp.runveal.permission.RequireLocationPermission])과 모양이 같지만 한
 * 가지가 다르다: 권한 쪽은 지도를 **항상** 그리고 그 위에 안내 시트를 얹는 반면, 여기서는 내용을
 * 아예 그리지 않는다. 네트워크 없이는 보여줄 내용 자체가 없는 화면에 쓰는 것이기 때문이다
 * (지도처럼 캐시로 일부라도 보여줄 수 있는 화면은 이걸로 감싸면 안 된다 — 그런 화면은
 * [rememberNetworkState]를 직접 구독해 배너 같은 걸 얹는 쪽이 맞다).
 *
 * @param onBackClick 뒤로가기. null이면 오류 화면에 상단 앱바를 그리지 않는다(탭 안쪽 등).
 */
@Composable
fun RequireNetwork(
    modifier: Modifier = Modifier,
    state: NetworkState = rememberNetworkState(),
    onBackClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (state.isConnected) {
        content()
        return
    }
    NetworkErrorScreen(
        // 연결이 돌아왔는지 다시 읽는다. 연결됐으면 isConnected가 true로 바뀌어 이 화면이 내려가고
        // content로 돌아간다 — 버튼이 따로 "숨기기"를 하지 않는다.
        onRetryClick = state::refresh,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
