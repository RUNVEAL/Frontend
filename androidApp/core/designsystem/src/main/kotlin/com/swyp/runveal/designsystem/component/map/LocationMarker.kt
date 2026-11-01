package com.swyp.runveal.designsystem.component.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 현재 위치 점. 지도 마커 이미지로 사용. */
@Composable
fun RunvealLocationMarker(modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(shape)
            .background(colors.map.location, shape)
            .border(2.dp, colors.icon.primary, shape),
    )
}

@Preview
@Composable
private fun RunvealLocationMarkerPreview() {
    RunvealTheme {
        RunvealLocationMarker()
    }
}
