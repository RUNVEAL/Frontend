package com.swyp.runveal.designsystem.component.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * 위치 권한이 꺼져 있으면 [banner]에 [RunvealLocationOffBanner]를 넣는다 —
 * [summary]를 대체하지 않고 그 위에 구분선과 함께 얹힌다.
 */
@Composable
fun RunvealHomeBottomPanel(
    summary: @Composable () -> Unit,
    tabBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    banner: (@Composable () -> Unit)? = null,
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
        if (banner != null) {
            banner()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.spacing.space20)
                    .height(1.dp)
                    .background(RunvealTheme.colors.border.default),
            )
        }
        summary()
        tabBar()
    }
}

/**
 * 위치 권한/위치 서비스가 꺼져 있을 때 [RunvealHomeBottomPanel]의 [RunvealHomeBottomPanel.banner]
 * 자리에 넣는 한 줄짜리 안내 배너 (HomeBottomPanel state=location_off). 권한이 꺼진 경우와 위치
 * 서비스(GPS)가 꺼진 경우는 문구·행동이 달라 호출하는 쪽에서 [message]/[actionLabel]로 구분해 넣는다.
 */
@Composable
fun RunvealLocationOffBanner(
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = RunvealTheme.spacing.space20, end = RunvealTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // tint를 주면 location-off 아이콘의 사선이 두꺼워 보인다([[BottomSheetHeader.kt]] 참고) — 벡터
        // 자체 색(전경 F4EEEA + 배경 272626 knockout)을 그대로 쓴다.
        Icon(
            imageVector = RunvealIcons.LocationOff,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
        Text(
            message,
            style = RunvealTheme.typography.bodyMRegular,
            color = colors.text.secondary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .height(40.dp)
                .clickable(onClick = onActionClick)
                .padding(horizontal = RunvealTheme.spacing.space12),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                actionLabel,
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.accent,
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
            banner = {
                RunvealLocationOffBanner(
                    message = "위치 권한이 꺼져 있어요",
                    actionLabel = "허용하기",
                    onActionClick = {},
                )
            },
            summary = {
                RunvealSummaryCard(
                    dongName = "내 동네",
                    ratePercent = 0,
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
