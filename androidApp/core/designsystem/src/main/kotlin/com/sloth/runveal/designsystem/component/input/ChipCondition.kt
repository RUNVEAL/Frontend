package com.sloth.runveal.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.theme.runvealGlass

enum class RunvealChipConditionIcon { Check, Filter }
enum class RunvealChipConditionUsage { OnCard, OnMap }

/** 켜고 끄는 조건 칩(안 가본 길 우선, 러닝 필터). [label]이 없으면 아이콘만 보인다. */
@Composable
fun RunvealChipCondition(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: RunvealChipConditionIcon = RunvealChipConditionIcon.Check,
    usage: RunvealChipConditionUsage = RunvealChipConditionUsage.OnCard,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val iconVector = if (icon == RunvealChipConditionIcon.Check) RunvealIcons.Check else RunvealIcons.Filter
    val contentColor = if (checked) colors.accent.glow else colors.icon.tertiary
    val labelColor = if (checked) colors.text.accent else colors.text.tertiary
    val borderColor = if (checked) colors.accent.glow else colors.border.default

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
                        RunvealChipConditionUsage.OnCard -> Modifier.background(colors.surface.raised, shape)
                        RunvealChipConditionUsage.OnMap -> Modifier.runvealGlass(shape)
                    },
                )
                .border(1.dp, borderColor, shape)
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox)
                .padding(horizontal = RunvealTheme.spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = iconVector, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            if (label != null) {
                Text(
                    label,
                    style = RunvealTheme.typography.bodyMSemibold,
                    color = labelColor,
                    modifier = Modifier.padding(end = RunvealTheme.spacing.space4),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RunvealChipConditionPreview() {
    RunvealTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunvealChipCondition(checked = true, onCheckedChange = {}, label = "안 가본 길 우선")
            RunvealChipCondition(checked = false, onCheckedChange = {}, icon = RunvealChipConditionIcon.Filter)
        }
    }
}
