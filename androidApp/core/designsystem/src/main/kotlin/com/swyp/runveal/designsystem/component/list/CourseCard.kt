package com.swyp.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.R
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.basic.RunvealTag
import com.swyp.runveal.designsystem.component.stat.RunvealStat
import com.swyp.runveal.designsystem.component.stat.RunvealStatType
import com.swyp.runveal.designsystem.theme.RunvealTheme

enum class RunvealCourseCardState { Default, Selected }

/** 추천 코스 카드. 가로 스크롤, 카드 간격 16. 선택된 카드는 테두리 accent/default + 이름 text/accent. */
@Composable
fun RunvealCourseCard(
    name: String,
    tags: List<String>,
    distance: String,
    time: String,
    elevation: String,
    newRoad: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: RunvealCourseCardState = RunvealCourseCardState.Default,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    val borderColor = if (state == RunvealCourseCardState.Selected) colors.accent.default else colors.border.default
    val nameColor = if (state == RunvealCourseCardState.Selected) colors.text.accent else colors.text.primary

    Column(
        modifier = modifier
            .width(240.dp)
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(top = 16.dp, end = 12.dp, bottom = 20.dp, start = 20.dp),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = RunvealTheme.typography.heading3, color = nameColor, modifier = Modifier.weight(1f))
            Icon(imageVector = RunvealIcons.Chevron, contentDescription = null, tint = colors.icon.tertiary, modifier = Modifier.size(16.dp))
        }
        if (tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                tags.take(2).forEach { RunvealTag(label = it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12)) {
            RunvealStat(label = stringResource(R.string.course_card_stat_distance), value = distance, type = RunvealStatType.Small)
            RunvealStat(label = stringResource(R.string.course_card_stat_time), value = time, type = RunvealStatType.Small)
            RunvealStat(label = stringResource(R.string.course_card_stat_elevation), value = elevation, type = RunvealStatType.Small)
            RunvealStat(label = stringResource(R.string.course_card_stat_new_road), value = newRoad, type = RunvealStatType.Small)
        }
    }
}

@Preview
@Composable
private fun RunvealCourseCardPreview() {
    RunvealTheme {
        RunvealCourseCard(
            name = "성수동 한 바퀴",
            tags = listOf("공원", "순환"),
            distance = "5.21km",
            time = "32분",
            elevation = "12m",
            newRoad = "1.3km",
            onClick = {},
            state = RunvealCourseCardState.Selected,
        )
    }
}
