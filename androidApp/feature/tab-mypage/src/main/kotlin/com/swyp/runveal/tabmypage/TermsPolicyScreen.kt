package com.swyp.runveal.tabmypage

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.swyp.runveal.designsystem.component.list.RunvealListItem
import com.swyp.runveal.designsystem.component.list.RunvealListItemTrailing
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabmypage.R
import com.swyp.runveal.tabmypage.contract.TermsPolicyItem

/**
 * 약관 및 정책 목록(디자인 `WF_E2`).
 *
 * TODO(로그인 도입): **지금은 이 화면으로 가는 길이 없다.** 1차 출시 마이페이지는 약관을 목록
 *  화면으로 빼지 않고 그 자리에 펼쳐 두기 때문이다(`MyScreen.TermsPolicySection`). 로그인과
 *  함께 마이페이지가 원래 구성으로 돌아갈 때 이 화면을 다시 연결할지, 인라인 섹션으로 계속
 *  갈지 정한다. 그때까지 지우지 않고 둔다 — 라우트(`RunVealRoute.HomeTab.Terms`)도 그대로다.
 *
 * 문서는 아직 열 수 없어서 눌러도 안내 토스트만 띄운다 — 약관 목록 API
 * (`docs/api/API.md` §3.8)가 붙으면 받아 온 URL을 여는 자리다.
 *
 * 상태랄 게 앱 버전 하나뿐이고 그마저도 기기에서 바로 읽는 값이라 ViewModel을 두지 않았다.
 */
@Composable
internal fun TermsPolicyScreen(
    onBackClick: () -> Unit,
    onItemClick: (TermsPolicyItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = remember(context) { context.appVersionName() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = onBackClick,
            title = stringResource(R.string.terms_policy_title),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RunvealTheme.layout.marginScreen)
                .padding(top = RunvealTheme.spacing.space24),
        ) {
            TermsPolicyItem.entries.forEach { item ->
                RunvealListItem(
                    title = stringResource(item.labelRes),
                    onClick = { onItemClick(item) },
                    trailing = RunvealListItemTrailing.Chevron,
                )
            }

            // 앱 버전은 눌러서 여는 줄이 아니라 값을 보여주는 줄이다 — chevron 대신 값을 오른쪽에
            // 둔다. RunvealListItem의 TextButton 자리를 쓰되 누르면 아무 일도 하지 않는다.
            RunvealListItem(
                title = stringResource(R.string.terms_policy_app_version),
                onClick = {},
                trailing = RunvealListItemTrailing.TextButton,
                trailingButtonLabel = versionName,
            )
        }
    }
}
