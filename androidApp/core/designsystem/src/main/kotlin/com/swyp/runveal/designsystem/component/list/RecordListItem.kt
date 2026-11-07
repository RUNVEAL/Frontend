package com.swyp.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.component.button.runvealPressOverlay
import com.swyp.runveal.designsystem.theme.RunvealTheme

private val TimelineWidth = 8.dp
private val TimelineDotSize = 8.dp
private val TimelineLineWidth = 1.dp
private val TimelineLineHeight = 148.dp
private val ThumbnailSize = 80.dp

private val TimelineGradient = Brush.verticalGradient(
    0f to Color(0xFFFBEFBC).copy(alpha = 1f),
    0.59f to Color(0xFFDFD4A7).copy(alpha = 0.73f),
    1f to Color(0xFF958E70).copy(alpha = 0f),
)

/**
 * 기록 화면의 기록 목록 한 줄. 왼쪽 타임라인(점 + 그라데이션 선) + 날짜 + 기록 카드(썸네일 + 정보).
 * 리스트에서 쓸 때 아이템 사이 간격은 0으로 두고 이 컴포저블의 아래 여백(12)만으로 연결선을 잇는다.
 */
@Composable
fun RunvealRecordListItem(
    date: String,
    title: String,
    distance: String,
    time: String,
    pace: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    newRoad: String? = null,
    thumbnail: @Composable BoxScope.() -> Unit = {},
) {
    val colors = RunvealTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = RunvealTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Timeline()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            Text(date, style = RunvealTheme.typography.captionMRegular, color = colors.text.tertiary)
            RecordCard(
                title = title,
                distance = distance,
                time = time,
                pace = pace,
                newRoad = newRoad,
                onClick = onClick,
                thumbnail = thumbnail,
            )
        }
    }
}

@Composable
private fun Timeline() {
    val colors = RunvealTheme.colors
    val dotShape = RunvealTheme.shapes.full

    Box(modifier = Modifier.width(TimelineWidth).height(TimelineLineHeight + RunvealTheme.spacing.space4)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = RunvealTheme.spacing.space4)
                .width(TimelineLineWidth)
                .height(TimelineLineHeight)
                .background(TimelineGradient),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(TimelineDotSize)
                .clip(dotShape)
                .background(colors.accent.default, dotShape)
                .border(2.dp, colors.progress.filled, dotShape),
        )
    }
}

@Composable
private fun RecordCard(
    title: String,
    distance: String,
    time: String,
    pace: String,
    newRoad: String?,
    onClick: () -> Unit,
    thumbnail: @Composable BoxScope.() -> Unit,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, colors.border.default, shape)
            .runvealPressOverlay(shape, colors.state.pressedOnSurface, pressed)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(RunvealTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ThumbnailSize)
                .clip(RunvealTheme.shapes.radius8)
                .background(colors.bg.map, RunvealTheme.shapes.radius8)
                .border(1.dp, colors.border.subtle, RunvealTheme.shapes.radius8),
            content = thumbnail,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            Text(title, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
                Text(distance, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                Text(time, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                Text(pace, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            }
            if (newRoad != null) {
                Text(newRoad, style = RunvealTheme.typography.bodyMRegular, color = colors.text.accent)
            }
        }
    }
}

@Preview
@Composable
private fun RunvealRecordListItemPreview() {
    RunvealTheme {
        Column {
            RunvealRecordListItem(
                date = "11월 4일 수요일",
                title = "성수동 한 바퀴",
                distance = "5.21km",
                time = "31:40",
                pace = "6′05″",
                newRoad = "새 길 +1.30km",
                onClick = {},
            )
            RunvealRecordListItem(
                date = "11월 2일 월요일",
                title = "저녁 러닝",
                distance = "3.02km",
                time = "18:12",
                pace = "6′02″",
                onClick = {},
            )
        }
    }
}
