package com.swyp.runveal.tabcourse.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.core.map.CourseSnapshotImage
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.map.snapshot.SnapshotRequest

/** 저장한 코스 목록의 한 줄 — 썸네일 + 정보 + 더보기 + 바로 시작. */
@Composable
fun SavedCourseListItem(
    title: String,
    distanceLabel: String,
    durationLabel: String,
    tag: String,
    runCountLabel: String,
    lastRunLabel: String,
    onMoreClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailRequest: SnapshotRequest? = null,
) {
    val colors = RunvealTheme.colors
    val thumbnailShape = RunvealTheme.shapes.radius8

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RunvealTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CourseSnapshotImage(
            request = thumbnailRequest,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(thumbnailShape)
                .background(colors.bg.map, thumbnailShape)
                .border(1.dp, colors.border.subtle, thumbnailShape),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
            Text(
                title,
                style = RunvealTheme.typography.bodyLSemibold,
                color = colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(distanceLabel, style = RunvealTheme.typography.bodyLRegular, color = colors.text.primary)
                Text(durationLabel, style = RunvealTheme.typography.bodyLRegular, color = colors.text.primary)
                Box(
                    modifier = Modifier
                        // 4dp 코너 반경 토큰이 없어 그대로 하드코딩.
                        .background(colors.surface.tag, RoundedCornerShape(4.dp))
                        .padding(horizontal = RunvealTheme.spacing.space4, vertical = RunvealTheme.spacing.space2),
                ) {
                    Text(tag, style = RunvealTheme.typography.captionSRegular, color = colors.text.secondary)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                verticalAlignment = Alignment.Top,
            ) {
                Text(runCountLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.tertiary)
                Text(lastRunLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.tertiary)
            }
        }

        Icon(
            imageVector = RunvealIcons.More,
            contentDescription = "더보기",
            tint = colors.icon.tertiary,
            modifier = Modifier.size(24.dp).clickable(onClick = onMoreClick),
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RunvealTheme.shapes.full)
                .background(colors.surface.default, RunvealTheme.shapes.full)
                .border(1.dp, colors.accent.default, RunvealTheme.shapes.full)
                .clickable(onClick = onStartClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RunvealIcons.Play,
                contentDescription = "바로 시작",
                tint = colors.accent.default,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SavedCourseListItemPreview() {
    RunvealTheme {
        SavedCourseListItem(
            title = "서울숲 근처 러닝",
            distanceLabel = "7.3km",
            durationLabel = "45분",
            tag = "순환",
            runCountLabel = "2회 달림",
            lastRunLabel = "최근 10.31",
            onMoreClick = {},
            onStartClick = {},
        )
    }
}
