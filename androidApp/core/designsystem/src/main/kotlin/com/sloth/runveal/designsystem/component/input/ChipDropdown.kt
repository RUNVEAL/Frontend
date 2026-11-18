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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.theme.runvealGlass

/** OnCard=카드 위(불투명), OnMap=지도 위(글래스). */
enum class RunvealChipDropdownUsage { OnCard, OnMap }

/**
 * 누르면 선택지가 열리는 칩. 라벨 + icon/expand 16.
 *
 * [onClick]이 null이면 **고른 값을 보여주기만 하는 칩**이 된다 — 화살표를 빼고 탭도 받지 않는다.
 * 열 선택지가 없는데 화살표만 달려 있으면 눌러 보게 되고, 눌러도 아무 일이 없어 고장으로 보인다.
 */
@Composable
fun RunvealChipDropdown(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    usage: RunvealChipDropdownUsage = RunvealChipDropdownUsage.OnCard,
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
                    when (usage) {
                        RunvealChipDropdownUsage.OnCard -> Modifier
                            .background(colors.surface.raised, shape)
                            .border(1.dp, colors.border.default, shape)
                        RunvealChipDropdownUsage.OnMap -> Modifier.runvealGlass(shape)
                    },
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = RunvealTheme.spacing.space16, end = RunvealTheme.spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.primary)
            if (onClick != null) {
                Icon(
                    imageVector = RunvealIcons.Expand,
                    contentDescription = null,
                    tint = colors.icon.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RunvealChipDropdownPreview() {
    RunvealTheme {
        RunvealChipDropdown(label = "거리순", onClick = {})
    }
}
