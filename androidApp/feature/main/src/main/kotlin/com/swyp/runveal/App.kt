package com.swyp.runveal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.swyp.runveal.designsystem.component.card.RunvealSummaryCard
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** HomeBottomPanel UI 테스트용 임시 화면 — 지도 자리에 캔버스 배경만 깔고 하단에 패널만 확인. */
@Composable
@Preview
fun App() {
    RunvealTheme {
        var selectedTab by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .background(RunvealTheme.colors.bg.canvas)
                .safeDrawingPadding()
                .fillMaxSize(),
        ) {
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
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        container = RunvealTabBarContainer.Embedded,
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = RunvealTheme.layout.marginHomePanel),
            )
        }
    }
}
