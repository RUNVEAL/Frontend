package com.swyp.runveal.designsystem.component.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 코스 성격 라벨(공원, 하천길, 순환, 편도). 누를 수 없음. */
@Composable
fun RunvealTag(label: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface.tag, shape)
            .padding(vertical = RunvealTheme.spacing.space2, horizontal = RunvealTheme.spacing.space4),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = RunvealTheme.typography.captionSRegular, color = colors.text.secondary)
    }
}

@Preview
@Composable
private fun RunvealTagPreview() {
    RunvealTheme {
        RunvealTag(label = "공원")
    }
}
