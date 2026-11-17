package com.sloth.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.basic.RunvealTag
import com.sloth.runveal.designsystem.component.stat.RunvealStat
import com.sloth.runveal.designsystem.component.stat.RunvealStatType
import com.sloth.runveal.designsystem.theme.RunvealTheme

enum class RunvealCourseCardState { Default, Selected }

/** 추천 코스 카드. 가로 스크롤, 카드 간격 16. 선택된 카드는 테두리 accent/default + 이름 text/accent. */
@Composable
fun RunvealCourseCard(
    name: String,
    tags: List<String>,
    distanceValue: String,
    distanceUnit: String,
    timeValue: String,
    timeUnit: String,
    elevationValue: String,
    elevationUnit: String,
    newRoadValue: String,
    newRoadUnit: String,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = RunvealTheme.typography.heading3, color = nameColor)
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4), verticalAlignment = Alignment.Bottom) {
                    tags.take(2).forEach { RunvealTag(label = it) }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = RunvealIcons.Chevron, contentDescription = null, tint = colors.icon.tertiary, modifier = Modifier.size(16.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12)) {
            // 수치가 없는 항목(고도, 새 길)도 공란으로 둘 뿐 칸을 차지하는 건 동일 — weight로 폭을 맞춰야 카드마다
            // 칸 위치가 어긋나지 않는다.
            RunvealStat(
                label = stringResource(R.string.course_card_stat_distance),
                value = distanceValue,
                unit = distanceUnit,
                type = RunvealStatType.Small,
                modifier = Modifier.weight(1f),
            )
            RunvealStat(
                label = stringResource(R.string.course_card_stat_time),
                value = timeValue,
                unit = timeUnit,
                type = RunvealStatType.Small,
                modifier = Modifier.weight(1f),
            )
            RunvealStat(
                label = stringResource(R.string.course_card_stat_elevation),
                value = elevationValue,
                unit = elevationUnit,
                type = RunvealStatType.Small,
                modifier = Modifier.weight(1f),
            )
            RunvealStat(
                label = stringResource(R.string.course_card_stat_new_road),
                value = newRoadValue,
                unit = newRoadUnit,
                type = RunvealStatType.Small,
                accent = true,
                modifier = Modifier.weight(1f),
            )
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
            distanceValue = "5.2",
            distanceUnit = "km",
            timeValue = "32",
            timeUnit = "분",
            elevationValue = "+45",
            elevationUnit = "m",
            newRoadValue = "1.3",
            newRoadUnit = "km",
            onClick = {},
            state = RunvealCourseCardState.Selected,
        )
    }
}
