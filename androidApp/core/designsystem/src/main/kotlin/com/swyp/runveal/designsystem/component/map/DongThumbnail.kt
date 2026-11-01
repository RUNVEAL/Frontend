package com.swyp.runveal.designsystem.component.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 동 썸네일 프레임. 내부 지도는 동마다 앱이 직접 그리므로([content] 슬롯), 여기서는
 * 배경/테두리 틀만 제공한다.
 */
@Composable
fun RunvealDongThumbnail(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius8
    Box(
        modifier = modifier
            .size(84.dp)
            .clip(shape)
            .background(colors.bg.map, shape)
            .border(1.dp, colors.border.default, shape),
        content = content,
    )
}

@Preview
@Composable
private fun RunvealDongThumbnailPreview() {
    RunvealTheme {
        RunvealDongThumbnail()
    }
}
