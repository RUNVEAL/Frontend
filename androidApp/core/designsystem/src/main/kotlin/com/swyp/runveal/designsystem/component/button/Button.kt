package com.swyp.runveal.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealGlass

/** Gold=러닝 이후 주요 행동, Secondary=일반 버튼, Danger=삭제처럼 되돌릴 수 없는 행동. */
enum class RunvealButtonStyle { Gold, Secondary, Danger }

@Composable
fun RunvealButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RunvealButtonStyle = RunvealButtonStyle.Gold,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val labelColor = when (style) {
        RunvealButtonStyle.Gold -> colors.text.onAccent
        RunvealButtonStyle.Secondary -> colors.text.primary
        RunvealButtonStyle.Danger -> colors.status.error
    }
    val pressOverlay = when (style) {
        RunvealButtonStyle.Gold -> colors.state.pressedOnAccent
        RunvealButtonStyle.Secondary, RunvealButtonStyle.Danger -> colors.state.pressedOnSurface
    }

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(RunvealTheme.layout.sizeButtonHeight)
            .clip(shape)
            .then(
                when (style) {
                    RunvealButtonStyle.Gold -> Modifier.background(colors.accent.default, shape)
                    RunvealButtonStyle.Secondary, RunvealButtonStyle.Danger -> Modifier.runvealGlass(shape)
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
        Text(label, style = RunvealTheme.typography.labelButton, color = labelColor)
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
