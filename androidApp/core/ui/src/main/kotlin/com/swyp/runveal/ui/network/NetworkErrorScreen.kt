package com.swyp.runveal.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.swyp.runveal.devicestate.NetworkState
import com.swyp.runveal.devicestate.rememberNetworkState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.core.ui.R
import com.swyp.runveal.designsystem.component.list.RunvealEmptyState
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 와이파이·모바일 데이터가 없어 아무 요청도 보낼 수 없을 때 내용 대신 보여주는 화면
 * (디자인 `Z1-1 네트워크 오류`). 레이아웃은 전부 [RunvealEmptyState]가 맡는다 — 시안의
 * `List/Empty State`가 그 컴포넌트와 같은 구성(80dp 그래픽 → 제목 + 설명 → Gold M 버튼)이다.
 *
 * 연결 여부는 이 화면이 직접 보지 않는다 — 띄울지 말지는 [rememberNetworkState]를 구독하는
 * 호출부가 정한다. 연결이 돌아오면 그 상태가 바뀌면서 화면이 알아서 내려간다.
 *
 * @param onRetryClick "다시 시도" — 연결이 돌아왔을 때 다시 해야 할 일은 화면마다 다르므로
 *  (목록 다시 불러오기, 매칭 재요청 등) 호출부가 정한다. 연결 자체는 버튼으로 복구할 수 없다.
 * @param onBackClick 뒤로가기. null이면 상단 앱바를 그리지 않는다 — 탭 안쪽처럼 돌아갈 곳이
 *  없는 자리에 그대로 끼워 넣을 때 쓴다.
 */
@Composable
fun NetworkErrorScreen(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas),
    ) {
        if (onBackClick != null) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onBackClick,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )
        }

        RunvealEmptyState(
            title = stringResource(R.string.network_error_title),
            description = stringResource(R.string.network_error_description),
            actionLabel = stringResource(R.string.network_error_retry),
            onActionClick = onRetryClick,
            // TODO: 시안의 `Graphic/Error`(80dp) 에셋이 아직 없어서 그래픽 없이 둔다. 받으면
            //  graphic = { Image(...) }로 넣으면 된다 — RunvealEmptyState가 80dp 자리를 이미 잡아 둔다.
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = RunvealTheme.layout.marginScreen),
        )
    }
}

@Preview
@Composable
private fun NetworkErrorScreenPreview() {
    RunvealTheme {
        NetworkErrorScreen(onRetryClick = {}, onBackClick = {})
    }
}
