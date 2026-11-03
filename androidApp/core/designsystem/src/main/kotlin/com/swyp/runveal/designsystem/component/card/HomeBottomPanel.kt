package com.swyp.runveal.designsystem.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.R
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.theme.runvealGlass

/**
 * 홈 하단 패널. GNB 위치는 다른 탭과 같게 고정, 패널이 바깥에서 감싼다(너비 = GNB + 좌우 6).
 * 보통 [summary]엔 [RunvealSummaryCard], [tabBar]엔
 * `RunvealTabBar(container = RunvealTabBarContainer.Embedded)`를 넣는다.
 */
@Composable
fun RunvealHomeBottomPanel(
    summary: @Composable () -> Unit,
    tabBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RunvealTheme.shapes.radius32
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .runvealGlass(shape)
            .padding(PaddingValues(top = RunvealTheme.spacing.space12, start = 6.dp, end = 6.dp, bottom = 6.dp)),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
    ) {
        summary()
        tabBar()
    }
}

/**
 * 위치 권한이 없을 때 [RunvealHomeBottomPanel]의 [summary] 자리에 넣는 안내 영역
 * (HomeBottomPanel state=location_off).
 */
@Composable
fun RunvealHomeLocationPermission(
    onAllowClick: () -> Unit,
    onLoadRunHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 14.dp, bottom = 8.dp, start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
    ) {
        Text(
            stringResource(R.string.home_location_permission_message),
            style = RunvealTheme.typography.bodyLSemibold,
            color = RunvealTheme.colors.text.primary,
            modifier = Modifier.padding(start = RunvealTheme.spacing.space12),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
            RunvealButton(
                label = stringResource(R.string.home_location_permission_load_history),
                onClick = onLoadRunHistoryClick,
                style = RunvealButtonStyle.Text,
            )
            RunvealButton(
                label = stringResource(R.string.home_location_permission_allow),
                onClick = onAllowClick,
                style = RunvealButtonStyle.Gold,
                size = RunvealButtonSize.L,
                fillWidth = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealHomeBottomPanelPreview() {
    RunvealTheme {
        RunvealHomeBottomPanel(
            summary = {
                RunvealSummaryCard(
                    dongName = "성수동",
                    ratePercent = 37,
                    onClick = {},
                    isMine = true,
                )
            },
            tabBar = {
                RunvealTabBar(
                    items = listOf(
                        RunvealTabItemSpec(RunvealIcons.Home, "홈"),
                        RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
                        RunvealTabItemSpec(RunvealIcons.Record, "기록"),
                        RunvealTabItemSpec(RunvealIcons.My, "MY"),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                    container = RunvealTabBarContainer.Embedded,
                )
            },
        )
    }
}

@Preview
@Composable
private fun RunvealHomeBottomPanelLocationOffPreview() {
    RunvealTheme {
        RunvealHomeBottomPanel(
            summary = {
                RunvealHomeLocationPermission(onAllowClick = {}, onLoadRunHistoryClick = {})
            },
            tabBar = {
                RunvealTabBar(
                    items = listOf(
                        RunvealTabItemSpec(RunvealIcons.Home, "홈"),
                        RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
                        RunvealTabItemSpec(RunvealIcons.Record, "기록"),
                        RunvealTabItemSpec(RunvealIcons.My, "MY"),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                    container = RunvealTabBarContainer.Embedded,
                )
            },
        )
    }
}
