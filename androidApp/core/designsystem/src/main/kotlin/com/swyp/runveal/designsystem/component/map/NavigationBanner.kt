package com.swyp.runveal.designsystem.component.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealGlass

/**
 * 러닝 중 길 안내 배너. [icon]은 좌회전 / 우회전 / 일시정지 중 하나를 문구와 함께 바꿔 전달한다
 * (design/icons에 turn-left/turn-right/pause SVG가 아직 없어 RunvealIcons로 고정하지 않았다 —
 * 추가되면 RunvealIcons.TurnLeft 등을 그대로 넘기면 된다). 보조 정보가 없으면 [description]을 생략한다.
 */
@Composable
fun RunvealNavigationBanner(
    icon: ImageVector,
    instruction: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .runvealGlass(shape)
            .padding(RunvealTheme.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.icon.primary, modifier = Modifier.size(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
            Text(instruction, style = RunvealTheme.typography.heading3, color = colors.text.primary)
            if (description != null) {
                Text(description, style = RunvealTheme.typography.captionMRegular, color = colors.text.secondary)
            }
        }
    }
}

@Preview
@Composable
private fun RunvealNavigationBannerPreview() {
    RunvealTheme {
        RunvealNavigationBanner(icon = RunvealIcons.Close, instruction = "좌회전", description = "50m 앞")
    }
}
