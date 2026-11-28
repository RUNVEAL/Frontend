package com.sloth.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
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

    // 시안(List/Course Card)은 내용 묶음과 셰브론이 나란히 선 가로 구성이다 — 셰브론은 제목 줄이
    // 아니라 **카드 세로 가운데**에 온다.
    Row(
        modifier = modifier
            .width(240.dp)
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(top = 16.dp, end = 12.dp, bottom = 20.dp, start = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    style = RunvealTheme.typography.heading3,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // fill = false: 이름이 짧으면 그만큼만 쓰고, 길면 태그를 밀어내는 대신 자기가 줄어든다.
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4), verticalAlignment = Alignment.Bottom) {
                        tags.take(2).forEach { RunvealTag(label = it) }
                    }
                }
            }
            CourseCardStats(
                entries = listOf(
                    CourseCardStat(stringResource(R.string.course_card_stat_distance), distanceValue, distanceUnit),
                    CourseCardStat(stringResource(R.string.course_card_stat_time), timeValue, timeUnit),
                    CourseCardStat(stringResource(R.string.course_card_stat_elevation), elevationValue, elevationUnit),
                    CourseCardStat(stringResource(R.string.course_card_stat_new_road), newRoadValue, newRoadUnit, accent = true),
                ),
            )
        }

        Icon(
            imageVector = RunvealIcons.Chevron,
            contentDescription = null,
            tint = colors.icon.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}

private class CourseCardStat(
    val label: String,
    val value: String,
    val unit: String,
    val accent: Boolean = false,
)

/**
 * 카드 안의 수치 네 개. 시안(Stats 기본/축소)대로 **각 수치는 제 내용만큼만 차지하고 남는 공간은
 * 오른쪽에 둔다** — 칸을 균등 분할하면 "28.7km"와 "+145m"처럼 길이가 다른 값이 제 칸 안에서
 * 왼쪽으로 몰려 수치 사이가 들쭉날쭉해 보인다.
 *
 * 대신 균등 분할이 해 주던 일(네 칸이 넘치지 않게 하는 것)을 직접 해야 한다. 그래서 먼저 네 수치의
 * 제 폭을 재서 간격까지 더한 값이 쓸 수 있는 폭을 넘으면 **축소 모드**로 간다 — 간격을 12에서 8로
 * 줄이고 값(숫자+단위)만 한 단계 작게 쓴다(라벨은 그대로). 시안의 "Stat 너비 합 + 36 > 178"이
 * 이 조건으로, 178 같은 숫자를 박아 두는 대신 실제로 주어진 폭과 비교한다 — 카드 폭이나 좌우
 * 여백이 바뀌어도 조건이 따라온다.
 */
@Composable
private fun CourseCardStats(entries: List<CourseCardStat>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = RunvealTheme.typography.captionSRegular
    val valueStyle = RunvealTheme.typography.numS
    val unitStyle = RunvealTheme.typography.unitS
    val gap = RunvealTheme.spacing.space12
    val compactGap = RunvealTheme.spacing.space8

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val available = maxWidth
        // 수치 하나의 제 폭은 라벨과 "값+단위" 중 넓은 쪽이다(Small은 값과 단위 사이 간격이 0).
        val natural = with(LocalDensity.current) {
            entries.sumOf { entry ->
                val label = measurer.measure(entry.label, labelStyle, softWrap = false).size.width
                val value = measurer.measure(entry.value, valueStyle, softWrap = false).size.width
                val unit = if (entry.unit.isEmpty()) {
                    0
                } else {
                    measurer.measure(entry.unit, unitStyle, softWrap = false).size.width
                }
                maxOf(label, value + unit)
            }.toDp()
        } + gap * (entries.size - 1)
        val isCompact = natural > available

        Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) compactGap else gap)) {
            entries.forEach { entry ->
                RunvealStat(
                    label = entry.label,
                    value = entry.value,
                    unit = entry.unit,
                    type = RunvealStatType.Small,
                    accent = entry.accent,
                    compactValue = isCompact,
                )
            }
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

/** 수치가 길어 축소 모드로 넘어가는 쪽 — 시안의 "Stats 축소" 예시 값이다. */
@Preview
@Composable
private fun RunvealCourseCardCompactStatsPreview() {
    RunvealTheme {
        RunvealCourseCard(
            name = "코스 A",
            tags = listOf("공원", "하천길"),
            distanceValue = "28.7",
            distanceUnit = "km",
            timeValue = "186",
            timeUnit = "분",
            elevationValue = "+145",
            elevationUnit = "m",
            newRoadValue = "25.0",
            newRoadUnit = "km",
            onClick = {},
        )
    }
}
