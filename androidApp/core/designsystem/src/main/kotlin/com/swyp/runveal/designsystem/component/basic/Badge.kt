package com.swyp.runveal.designsystem.component.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

enum class RunvealBadgeSize { L, M }
enum class RunvealBadgeTone { Default, Accent }

/** 누를 수 없는 강조 라벨. 라벨 + 값(선택). 예: 새로 탐험한 길 +0.8km. */
@Composable
fun RunvealBadge(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    size: RunvealBadgeSize = RunvealBadgeSize.M,
    tone: RunvealBadgeTone = RunvealBadgeTone.Default,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val typography = if (size == RunvealBadgeSize.L) RunvealTheme.typography.bodyMSemibold else RunvealTheme.typography.captionSSemibold
    val gap = if (size == RunvealBadgeSize.L) RunvealTheme.spacing.space8 else 6.dp
    val paddingVertical = if (size == RunvealBadgeSize.L) RunvealTheme.spacing.space8 else 6.dp
    val paddingHorizontal = if (size == RunvealBadgeSize.L) RunvealTheme.spacing.space12 else 10.dp
    val labelColor = if (tone == RunvealBadgeTone.Accent) colors.text.accent else colors.text.secondary

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface.raised, shape)
            .padding(vertical = paddingVertical, horizontal = paddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = typography, color = labelColor)
        if (value != null) {
            Text(value, style = typography, color = colors.text.accent)
        }
    }
}

@Preview
@Composable
private fun RunvealBadgePreview() {
    RunvealTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunvealBadge(label = "새로 탐험한 길", value = "+0.8km", size = RunvealBadgeSize.L)
            RunvealBadge(label = "새로 탐험한 길", value = "+0.8km", size = RunvealBadgeSize.M, tone = RunvealBadgeTone.Accent)
        }
    }
}
