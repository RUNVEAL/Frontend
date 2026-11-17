package com.sloth.runveal.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 러닝 중 일시정지 원형 버튼. [RunvealRunStartButton]보다 작다(80). */
@Composable
fun RunvealRunPauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.run_pause_button_default_label),
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(shape)
            .background(colors.accent.default, shape)
            .runvealPressOverlay(shape, colors.state.pressedOnAccent, pressed)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = RunvealIcons.Pause, contentDescription = label, tint = colors.icon.onAccent)
            Text(label, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.onAccent)
        }
    }
}

@Preview
@Composable
private fun RunvealRunPauseButtonPreview() {
    RunvealTheme {
        RunvealRunPauseButton(onClick = {})
    }
}
