package com.sloth.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.basic.RunvealTag
import com.sloth.runveal.designsystem.theme.RunvealTheme

enum class RunvealCourseListItemRunState { Run, NotRun }

/** 내 코스 목록 한 줄. 아직 안 달린 코스는 상태 글자를 text/accent로 강조. */
@Composable
fun RunvealCourseListItem(
    title: String,
    distance: String,
    time: String,
    runState: RunvealCourseListItemRunState,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailType: RunvealCourseThumbnailType = RunvealCourseThumbnailType.OneWay,
    tag: String? = null,
    runCount: String? = null,
    lastRun: String? = null,
    showDivider: Boolean = false,
) {
    val colors = RunvealTheme.colors

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = RunvealTheme.spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RunvealCourseThumbnail(type = thumbnailType)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
                Text(title, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8), verticalAlignment = Alignment.CenterVertically) {
                    Text(distance, style = RunvealTheme.typography.bodyLRegular, color = colors.text.primary)
                    Text(time, style = RunvealTheme.typography.bodyLRegular, color = colors.text.primary)
                    if (tag != null) RunvealTag(label = tag)
                }
                if (runState == RunvealCourseListItemRunState.Run) {
                    Text(
                        listOfNotNull(runCount, lastRun).joinToString(" · "),
                        style = RunvealTheme.typography.bodyMRegular,
                        color = colors.text.tertiary,
                    )
                } else {
                    Text(
                        stringResource(R.string.course_list_item_not_run),
                        style = RunvealTheme.typography.bodyMRegular,
                        color = colors.text.accent,
                    )
                }
            }

            Box(
                modifier = Modifier.size(RunvealTheme.layout.sizeTouchTargetMin).clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = RunvealIcons.More, contentDescription = null, tint = colors.icon.primary, modifier = Modifier.size(RunvealTheme.layout.sizeIcon))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RunvealTheme.shapes.full)
                    .background(colors.surface.default, RunvealTheme.shapes.full)
                    .border(1.dp, colors.accent.default, RunvealTheme.shapes.full)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = RunvealIcons.Play, contentDescription = null, tint = colors.icon.primary, modifier = Modifier.size(16.dp))
            }
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border.subtle))
        }
    }
}

@Preview
@Composable
private fun RunvealCourseListItemPreview() {
    RunvealTheme {
        RunvealCourseListItem(
            title = "성수동 한 바퀴",
            distance = "5.21km",
            time = "32:10",
            tag = "순환",
            runState = RunvealCourseListItemRunState.Run,
            runCount = "3회 완주",
            lastRun = "2일 전",
            onClick = {},
            onMoreClick = {},
            onPlayClick = {},
            thumbnailType = RunvealCourseThumbnailType.Circulation,
        )
    }
}
