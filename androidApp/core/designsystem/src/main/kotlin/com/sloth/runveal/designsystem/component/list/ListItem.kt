package com.sloth.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.component.input.RunvealCheckbox
import com.sloth.runveal.designsystem.component.input.RunvealCheckboxState
import com.sloth.runveal.designsystem.theme.RunvealTheme

enum class RunvealListItemSize { L, M }
enum class RunvealListItemTrailing { None, Chevron, TextButton }

/**
 * 단순한 리스트 줄. 줄 전체가 터치 영역. [leadingIcon]/[leadingCheckbox]/[leadingNumber] 중
 * 하나만 쓴다(없으면 왼쪽 여백 없음).
 */
@Composable
fun RunvealListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: ImageVector? = null,
    leadingCheckbox: RunvealCheckboxState? = null,
    leadingNumber: String? = null,
    size: RunvealListItemSize = RunvealListItemSize.M,
    trailing: RunvealListItemTrailing = RunvealListItemTrailing.None,
    trailingButtonLabel: String? = null,
    onTrailingButtonClick: () -> Unit = {},
    showDivider: Boolean = false,
) {
    val colors = RunvealTheme.colors
    val minHeight = if (size == RunvealListItemSize.L) 64.dp else 52.dp
    val paddingVertical = if (size == RunvealListItemSize.L) RunvealTheme.spacing.space12 else RunvealTheme.spacing.space2

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .clickable(onClick = onClick)
                .padding(vertical = paddingVertical),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                leadingIcon != null -> Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.icon.primary,
                    modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
                )
                leadingCheckbox != null -> RunvealCheckbox(state = leadingCheckbox)
                leadingNumber != null -> Text(leadingNumber, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.accent)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (size == RunvealListItemSize.L) RunvealTheme.typography.bodyLSemibold else RunvealTheme.typography.bodyMRegular,
                    color = if (size == RunvealListItemSize.L) colors.text.primary else colors.text.secondary,
                )
                if (description != null) {
                    Text(description, style = RunvealTheme.typography.captionMRegular, color = colors.text.tertiary)
                }
            }

            when (trailing) {
                RunvealListItemTrailing.Chevron -> Icon(
                    imageVector = RunvealIcons.Chevron,
                    contentDescription = null,
                    tint = colors.icon.tertiary,
                    modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
                )
                RunvealListItemTrailing.TextButton -> RunvealButton(
                    label = trailingButtonLabel.orEmpty(),
                    onClick = onTrailingButtonClick,
                    style = RunvealButtonStyle.Text,
                )
                RunvealListItemTrailing.None -> Unit
            }
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border.subtle))
        }
    }
}

@Preview
@Composable
private fun RunvealListItemPreview() {
    RunvealTheme {
        Column {
            RunvealListItem(
                title = "코스 이름",
                description = "성수동 · 5.21km",
                onClick = {},
                leadingIcon = RunvealIcons.Course,
                size = RunvealListItemSize.L,
                trailing = RunvealListItemTrailing.Chevron,
                showDivider = true,
            )
            RunvealListItem(
                title = "약관에 동의합니다",
                onClick = {},
                leadingCheckbox = RunvealCheckboxState.Checked,
            )
        }
    }
}
