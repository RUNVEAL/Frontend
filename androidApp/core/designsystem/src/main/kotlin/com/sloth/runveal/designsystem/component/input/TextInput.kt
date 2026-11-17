package com.sloth.runveal.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 한 줄 입력(코스 이름 등). focused/filled 같은 시각 상태는 포커스·[value]로 자동 반영되고, [isError]/[enabled]만 직접 전달한다. */
@Composable
fun RunvealTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isError -> colors.status.error
        focused -> colors.accent.default
        else -> colors.border.default
    }
    val borderWidth = if (isError || focused) 1.5.dp else 1.dp

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Text(label, style = RunvealTheme.typography.captionMRegular, color = colors.text.secondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            interactionSource = interactionSource,
            textStyle = RunvealTheme.typography.bodyLRegular.copy(color = colors.text.primary),
            cursorBrush = SolidColor(colors.accent.default),
            modifier = Modifier
                .fillMaxWidth()
                .height(RunvealTheme.layout.sizeTextField)
                .clip(shape)
                .background(colors.surface.default, shape)
                .border(borderWidth, borderColor, shape),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RunvealTheme.spacing.space16),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = RunvealTheme.typography.bodyLRegular, color = colors.text.tertiary)
                    }
                    innerTextField()
                }
            },
        )
        if (helper != null) {
            Text(
                helper,
                style = RunvealTheme.typography.captionMRegular,
                color = if (isError) colors.status.error else colors.text.tertiary,
            )
        }
    }
}

@Preview
@Composable
private fun RunvealTextInputPreview() {
    RunvealTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RunvealTextInput(value = "", onValueChange = {}, label = "코스 이름", placeholder = "코스 이름을 입력하세요")
            RunvealTextInput(
                value = "성수동 러닝",
                onValueChange = {},
                label = "코스 이름",
                helper = "이미 있는 이름이에요",
                isError = true,
            )
        }
    }
}
