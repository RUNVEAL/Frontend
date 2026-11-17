package com.sloth.runveal.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 하나를 고르는 선택 칩(거리 5km 등). 보이는 높이는 32지만 터치 영역은 48 이상. */
@Composable
fun RunvealChipOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    Box(
        modifier = modifier.defaultMinSize(minHeight = RunvealTheme.layout.sizeTouchTargetMin),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(RunvealTheme.layout.sizeChipHeight)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(colors.surface.inverse, shape)
                    } else {
                        Modifier.border(1.dp, colors.border.strong, shape)
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = RunvealTheme.spacing.space12),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = RunvealTheme.typography.bodyMRegular,
                color = if (selected) colors.text.onInverse else colors.text.primary,
            )
        }
    }
}

@Preview
@Composable
private fun RunvealChipOptionPreview() {
    RunvealTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunvealChipOption(label = "5km", selected = true, onClick = {})
            RunvealChipOption(label = "10km", selected = false, onClick = {})
        }
    }
}
