package com.swyp.runveal.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealGlass

/** Gold=주요 행동, Secondary=일반 버튼, Danger=삭제처럼 되돌릴 수 없는 행동, Text=배경 없는 보조 행동. */
enum class RunvealButtonStyle { Gold, Secondary, Danger, Text }

/** L=하단·시트 버튼, M=화면 안 인라인 버튼(빈 상태 등, Gold/Secondary만). Text는 둘 다 같은 모양이라 영향 없음. */
enum class RunvealButtonSize { L, M }

@Composable
fun RunvealButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RunvealButtonStyle = RunvealButtonStyle.Gold,
    size: RunvealButtonSize = RunvealButtonSize.L,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    leadingIcon: ImageVector? = null,
    /** 카카오·구글 로고처럼 색이 여러 개라 원본 색을 살려야 하는 아이콘은 false로 준다. */
    tintLeadingIcon: Boolean = true,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    if (style == RunvealButtonStyle.Text) {
        Row(
            modifier = modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minWidth = RunvealTheme.layout.sizeTouchTargetMin, minHeight = RunvealTheme.layout.sizeTouchTargetMin)
                .clip(shape)
                .runvealPressOverlay(shape, colors.state.pressedOnSurface, pressed)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = RunvealTheme.spacing.space12)
                .alpha(if (enabled) 1f else 0.4f),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                val iconTint = if (tintLeadingIcon) colors.text.tertiary else Color.Unspecified
                Icon(leadingIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.tertiary)
        }
        return
    }

    val labelColor = when (style) {
        RunvealButtonStyle.Gold -> colors.text.onAccent
        RunvealButtonStyle.Secondary -> colors.text.primary
        RunvealButtonStyle.Danger -> colors.status.error
        RunvealButtonStyle.Text -> colors.text.tertiary
    }
    val pressOverlay = when (style) {
        RunvealButtonStyle.Gold -> colors.state.pressedOnAccent
        RunvealButtonStyle.Secondary, RunvealButtonStyle.Danger, RunvealButtonStyle.Text -> colors.state.pressedOnSurface
    }
    val height = when (size) {
        RunvealButtonSize.L -> RunvealTheme.layout.sizeButtonHeight
        RunvealButtonSize.M -> RunvealTheme.layout.sizeButtonHeightM
    }

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .then(if (size == RunvealButtonSize.M) Modifier.defaultMinSize(minWidth = RunvealTheme.layout.sizeButtonMinWidthM) else Modifier)
            .height(height)
            .clip(shape)
            .then(
                when (style) {
                    RunvealButtonStyle.Gold -> Modifier.background(colors.accent.default, shape)
                    RunvealButtonStyle.Secondary, RunvealButtonStyle.Danger -> Modifier.runvealGlass(shape)
                    RunvealButtonStyle.Text -> Modifier
                },
            )
            .runvealPressOverlay(shape, pressOverlay, pressed)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = RunvealTheme.spacing.space24)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                val iconTint = if (tintLeadingIcon) labelColor else Color.Unspecified
                Icon(leadingIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Text(label, style = RunvealTheme.typography.labelButton, color = labelColor)
        }
    }
}

@Preview
@Composable
private fun RunvealButtonPreview() {
    RunvealTheme {
        Box(Modifier.padding(16.dp)) {
            RunvealButton(label = "러닝 시작", onClick = {})
        }
    }
}

@Preview
@Composable
private fun RunvealButtonVariantsPreview() {
    RunvealTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunvealButton(label = "코스 만들기", onClick = {}, style = RunvealButtonStyle.Gold, size = RunvealButtonSize.M, leadingIcon = RunvealIcons.Plus)
            RunvealButton(label = "건너뛰기", onClick = {}, style = RunvealButtonStyle.Text)
        }
    }
}
