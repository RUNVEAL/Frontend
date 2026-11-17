package com.sloth.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.component.map.RunvealMapMarker
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerSize
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 코스 썸네일 72. 안쪽 지도와 경로는 [content] 슬롯(앱 Canvas 또는 서버 이미지)으로 그린다. */
enum class RunvealCourseThumbnailType { OneWay, Circulation }

@Composable
fun RunvealCourseThumbnail(
    type: RunvealCourseThumbnailType,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius8

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(shape)
            .background(colors.bg.map, shape)
            .border(1.dp, colors.border.subtle, shape),
    ) {
        content()
        when (type) {
            RunvealCourseThumbnailType.OneWay -> {
                RunvealMapMarker(
                    type = RunvealMapMarkerType.Start,
                    size = RunvealMapMarkerSize.Small,
                    modifier = Modifier.align(Alignment.TopStart).padding(RunvealTheme.spacing.space4),
                )
                RunvealMapMarker(
                    type = RunvealMapMarkerType.End,
                    size = RunvealMapMarkerSize.Small,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(RunvealTheme.spacing.space4),
                )
            }
            RunvealCourseThumbnailType.Circulation -> RunvealMapMarker(
                type = RunvealMapMarkerType.Start,
                size = RunvealMapMarkerSize.Small,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealCourseThumbnailPreview() {
    RunvealTheme {
        RunvealCourseThumbnail(type = RunvealCourseThumbnailType.OneWay)
    }
}
