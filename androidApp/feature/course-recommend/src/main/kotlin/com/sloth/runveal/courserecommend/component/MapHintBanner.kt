package com.sloth.runveal.courserecommend.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 지도 위에 뜨는 짧은 안내 배너 (출발/도착 선택 중 "지도를 탭해 ... 선택하세요" 등). */
@Composable
fun MapHintBanner(message: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RoundedCornerShape(RunvealTheme.radius.radius16)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.bg.scrim, shape)
            .border(1.dp, colors.border.subtle, shape)
            .padding(horizontal = RunvealTheme.spacing.space16, vertical = RunvealTheme.spacing.space12),
    ) {
        Text(
            message,
            style = RunvealTheme.typography.bodyMRegular,
            color = colors.text.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun MapHintBannerPreview() {
    RunvealTheme {
        MapHintBanner(message = "지도를 탭해 출발지를 선택하세요")
    }
}
