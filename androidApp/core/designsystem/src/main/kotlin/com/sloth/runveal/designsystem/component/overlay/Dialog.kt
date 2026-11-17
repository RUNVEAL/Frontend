package com.sloth.runveal.designsystem.component.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.theme.runvealGlass

/** Default=확인 Gold, Destructive=확인 Danger. */
enum class RunvealDialogType { Default, Destructive }

/**
 * 확인 다이얼로그. 뒤 bg/scrim은 호출하는 쪽의 다이얼로그 호스트가 맡는다 —
 * 이 컴포저블은 다이얼로그 카드 모양만 그린다.
 */
@Composable
fun RunvealDialog(
    title: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    type: RunvealDialogType = RunvealDialogType.Default,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius24

    Column(
        modifier = modifier
            .width(RunvealTheme.layout.sizeDialogWidth)
            .clip(shape)
            .runvealGlass(shape)
            .padding(RunvealTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
    ) {
        Column(
            modifier = Modifier.padding(RunvealTheme.spacing.space8),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            Text(title, style = RunvealTheme.typography.heading3, color = colors.text.primary)
            if (description != null) {
                Text(description, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
            RunvealButton(
                label = cancelLabel,
                onClick = onCancel,
                style = RunvealButtonStyle.Secondary,
                fillWidth = true,
                modifier = Modifier.weight(1f),
            )
            RunvealButton(
                label = confirmLabel,
                onClick = onConfirm,
                style = if (type == RunvealDialogType.Destructive) RunvealButtonStyle.Danger else RunvealButtonStyle.Gold,
                fillWidth = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealDialogPreview() {
    RunvealTheme {
        RunvealDialog(
            title = "코스를 삭제할까요?",
            description = "삭제하면 되돌릴 수 없어요",
            confirmLabel = "삭제",
            cancelLabel = "취소",
            onConfirm = {},
            onCancel = {},
            type = RunvealDialogType.Destructive,
        )
    }
}
