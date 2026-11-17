package com.sloth.runveal.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.theme.runvealGlass

/** 지도 보기 전환 토글(탐험한 길 / 안 가본 길). */
@Composable
fun RunvealSegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val containerShape = RunvealTheme.shapes.full

    Row(
        modifier = modifier
            .height(RunvealTheme.layout.sizeSegmentedToggle)
            .clip(containerShape)
            .runvealGlass(containerShape)
            .padding(RunvealTheme.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val segmentShape = RunvealTheme.shapes.full
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(segmentShape)
                    .background(if (selected) colors.accent.default else Color.Transparent, segmentShape)
                    .clickable(onClick = { onSelect(index) })
                    .padding(horizontal = RunvealTheme.spacing.space16),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = if (selected) RunvealTheme.typography.bodyMSemibold else RunvealTheme.typography.bodyMRegular,
                    color = if (selected) colors.text.onAccent else colors.text.tertiary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun RunvealSegmentedTogglePreview() {
    RunvealTheme {
        RunvealSegmentedToggle(options = listOf("탐험한 길", "안 가본 길"), selectedIndex = 0, onSelect = {})
    }
}
