package com.swyp.runveal.designsystem.component.input

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 켜고 끄는 스위치(안 가본 길 우선 추천 등). 터치 영역은 보통 행 전체가 받고, 비활성은 전체 40% 투명. */
@Composable
fun RunvealToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RunvealTheme.colors
    val trackShape = RoundedCornerShape(16.dp)
    val knobShape = RunvealTheme.shapes.full
    val trackColor = if (checked) colors.accent.default else colors.border.strong
    val knobOffset by animateDpAsState(if (checked) 20.dp else 0.dp)

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(trackShape)
            .background(trackColor, trackShape)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
            )
            .alpha(if (enabled) 1f else 0.4f)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(26.dp)
                .clip(knobShape)
                .background(colors.surface.raised, knobShape),
        )
    }
}

@Preview
@Composable
private fun RunvealToggleSwitchPreview() {
    RunvealTheme {
        RunvealToggleSwitch(checked = true, onCheckedChange = {})
    }
}
