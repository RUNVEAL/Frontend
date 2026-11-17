package com.sloth.runveal.designsystem.component.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.theme.runvealDropShadow

/** 지도 범례. 배경 없이 지도 위에 바로 올린다. */
enum class RunvealLegendItem { New, Today, Explored }

@Composable
fun RunvealLegend(
    modifier: Modifier = Modifier,
    items: List<RunvealLegendItem> = listOf(RunvealLegendItem.New, RunvealLegendItem.Today, RunvealLegendItem.Explored),
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12)) {
        items.forEach { item ->
            val (label, color) = when (item) {
                RunvealLegendItem.New -> stringResource(R.string.legend_new) to colors.map.roadNew
                RunvealLegendItem.Today -> stringResource(R.string.legend_today) to colors.map.roadToday
                RunvealLegendItem.Explored -> stringResource(R.string.legend_explored) to colors.map.roadExplored
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var swatch: Modifier = Modifier
                    .size(width = 14.dp, height = 3.dp)
                    .clip(shape)
                if (item == RunvealLegendItem.New) {
                    swatch = swatch.runvealDropShadow(shape, RunvealTheme.effects.glowGold)
                }
                Box(swatch.background(color, shape))
                Text(label, style = RunvealTheme.typography.captionMRegular, color = colors.text.secondary)
            }
        }
    }
}

@Preview
@Composable
private fun RunvealLegendPreview() {
    RunvealTheme {
        RunvealLegend()
    }
}
