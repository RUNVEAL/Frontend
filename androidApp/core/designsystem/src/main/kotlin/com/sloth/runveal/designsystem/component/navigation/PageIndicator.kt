package com.sloth.runveal.designsystem.component.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 온보딩 단계 표시. */
@Composable
fun RunvealPageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 20.dp else 6.dp)
            Box(
                modifier = Modifier
                    .size(width = width, height = 6.dp)
                    .clip(shape)
                    .background(if (active) colors.icon.primary else colors.border.strong, shape),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealPageIndicatorPreview() {
    RunvealTheme {
        RunvealPageIndicator(count = 3, current = 1)
    }
}
