package com.swyp.runveal.designsystem.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
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
