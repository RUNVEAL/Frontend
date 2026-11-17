package com.sloth.runveal.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.theme.runvealGlass

data class RunvealTabItemSpec(
    val icon: ImageVector,
    val label: String,
)

/** Floating=단독(글래스), Embedded=Home Bottom Panel 안(글래스 효과는 바깥 패널이 담당). */
enum class RunvealTabBarContainer { Floating, Embedded }

/** 플로팅 GNB. */
@Composable
fun RunvealTabBar(
    items: List<RunvealTabItemSpec>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    container: RunvealTabBarContainer = RunvealTabBarContainer.Floating,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RunvealTheme.layout.sizeTabBar)
            .clip(shape)
            .then(
                when (container) {
                    RunvealTabBarContainer.Floating -> Modifier.runvealGlass(shape)
                    RunvealTabBarContainer.Embedded -> Modifier
                        .background(colors.surface.default, shape)
                        .border(1.dp, colors.border.default, shape)
                },
            )
            .padding(RunvealTheme.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            RunvealTabItem(
                icon = item.icon,
                label = item.label,
                active = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealTabBarPreview() {
    RunvealTheme {
        RunvealTabBar(
            items = listOf(
                RunvealTabItemSpec(RunvealIcons.Home, "홈"),
                RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
                RunvealTabItemSpec(RunvealIcons.Record, "기록"),
                RunvealTabItemSpec(RunvealIcons.My, "MY"),
            ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
