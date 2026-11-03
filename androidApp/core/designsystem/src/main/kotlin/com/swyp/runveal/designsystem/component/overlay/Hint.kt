package com.swyp.runveal.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 계속 떠 있는 안내(지도를 탭해 출발지를 선택하세요). 글자 길이만큼 너비 — 터치는 통과시킨다(클릭 리스너를 달지 않는다). */
@Composable
fun RunvealHint(message: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.bg.scrim, shape)
            .border(1.dp, colors.border.subtle, shape)
            .padding(vertical = RunvealTheme.spacing.space12, horizontal = RunvealTheme.spacing.space16),
    ) {
        Text(message, style = RunvealTheme.typography.bodyMRegular, color = colors.text.primary)
    }
}

@Preview
@Composable
private fun RunvealHintPreview() {
    RunvealTheme {
        RunvealHint(message = "지도를 탭해 출발지를 선택하세요")
    }
}
