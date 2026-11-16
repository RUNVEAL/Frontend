package com.swyp.runveal.tabmypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabmypage.R

/** `health.png`의 원본 크기(240x240px, xxxhdpi 4배). */
private val HEALTH_LOGO_SIZE = 60.dp

/**
 * Health Connect 기록 불러오기 안내(디자인 `A4-1`). 무엇을 가져오고 무엇이 안 되는지 먼저 알리고
 * 동의를 받는 화면이라, 실제 가져오기는 "불러오기"를 누른 뒤에 시작한다.
 */
@Composable
internal fun ImportRecordsScreen(
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    onLaterClick: () -> Unit,
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
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = RunvealTheme.layout.marginScreen)
                .padding(top = RunvealTheme.spacing.space24),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
        ) {
            // 색이 들어 있는 그래픽이라 Icon(tint)이 아니라 Image로 그린다.
            // design/images/health.png — 240x240px(= 60dp, xxxhdpi 4배).
            Image(
                painter = painterResource(R.drawable.health),
                contentDescription = null,
                modifier = Modifier.size(HEALTH_LOGO_SIZE),
            )

            Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
                Text(
                    text = stringResource(R.string.import_records_title),
                    style = RunvealTheme.typography.heading2,
                    color = RunvealTheme.colors.text.primary,
                )
                Text(
                    text = stringResource(R.string.import_records_description),
                    style = RunvealTheme.typography.bodyMRegular,
                    color = RunvealTheme.colors.text.secondary,
                )
            }

            NoticeCard(
                notices = listOf(
                    stringResource(R.string.import_records_notice_what),
                    stringResource(R.string.import_records_notice_no_route),
                    stringResource(R.string.import_records_notice_disconnect),
                ),
            )
        }

        RunvealButtonGroup(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            RunvealButtonGroupPrimaryText(
                primaryLabel = stringResource(R.string.import_records_start),
                onPrimaryClick = onImportClick,
                textLabel = stringResource(R.string.import_records_later),
                onTextClick = onLaterClick,
            )
        }
    }
}

/**
 * 안내 문구 묶음. 위치 권한 시트(`LocationPermissionSheet`)에 같은 모양이 private으로 하나 더 있다 —
 * 배경색이 달라(그쪽은 시트 위라 `bg.canvas`) 공용화하지 않았다. 세 번째 쓰임이 생기면 그때
 * 디자인시스템으로 올리는 게 맞다.
 */
@Composable
private fun NoticeCard(notices: List<String>, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.default, RunvealTheme.shapes.radius16)
            .padding(RunvealTheme.layout.marginCardPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
            notices.forEach { notice ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 불릿은 색이 들어 있는 벡터라 tint를 주지 않는다.
                    Icon(
                        imageVector = RunvealIcons.Bullet,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp),
                    )
                    Text(
                        text = notice,
                        style = RunvealTheme.typography.bodyMRegular,
                        color = colors.text.secondary,
                    )
                }
            }
        }
    }
}
