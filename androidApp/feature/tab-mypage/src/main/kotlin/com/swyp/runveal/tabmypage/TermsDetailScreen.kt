package com.swyp.runveal.tabmypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.tabmypage.contract.TermsPolicyItem

/**
 * 약관 한 건의 본문. 상단 제목은 목록에서 누른 약관 이름([TermsPolicyItem])을 그대로 쓴다.
 *
 * TODO: 본문은 비워 뒀다 — 약관 목록 API(`docs/api/API.md` §3.8)가 붙으면 받아 온 내용을 여기
 *  채운다. 본문이 길어질 것을 전제로 스크롤은 미리 걸어 뒀다.
 */
@Composable
internal fun TermsDetailScreen(
    item: TermsPolicyItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = onBackClick,
            title = stringResource(item.labelRes),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RunvealTheme.layout.marginScreen)
                .padding(top = RunvealTheme.spacing.space24),
        ) {
            // 본문 자리 — 위 TODO 참고.
        }
    }
}
