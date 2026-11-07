package com.swyp.runveal.courserecommend.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.component.input.RunvealChipCondition
import com.swyp.runveal.designsystem.component.input.RunvealChipConditionIcon
import com.swyp.runveal.designsystem.component.input.RunvealChipDropdown
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 홈 하단에 뜨는 코스 만들기 패널 — 출발/도착 설정, 거리·조건 칩, 경로 추천 받기 버튼. */
@Composable
fun MakeCoursePanel(
    startLocation: String,
    endLocation: String,
    distanceLabel: String,
    avoidVisitedPreferred: Boolean,
    onStartLocationClick: () -> Unit,
    onEndLocationClick: () -> Unit,
    onDistanceClick: () -> Unit,
    onAvoidVisitedPreferredChange: (Boolean) -> Unit,
    onFilterClick: () -> Unit,
    onRecommendClick: () -> Unit,
    modifier: Modifier = Modifier,
    endLocationIsDefault: Boolean = true,
    selectedFilterCount: Int = 0,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius32

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, colors.border.default, shape)
            .padding(start = 6.dp, top = RunvealTheme.spacing.space16, end = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
        ) {
            RouteSettingRow(
                label = "출발",
                location = startLocation,
                locationColor = colors.text.primary,
                onClick = onStartLocationClick,
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border.default))
            RouteSettingRow(
                label = "도착",
                location = endLocation,
                locationColor = if (endLocationIsDefault) colors.text.secondary else colors.text.primary,
                onClick = onEndLocationClick,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            RunvealChipDropdown(label = distanceLabel, onClick = onDistanceClick)
            RunvealChipCondition(
                checked = avoidVisitedPreferred,
                onCheckedChange = onAvoidVisitedPreferredChange,
                label = "안 가본 길 우선",
            )
            RunvealChipCondition(
                checked = selectedFilterCount > 0,
                onCheckedChange = { onFilterClick() },
                icon = RunvealChipConditionIcon.Filter,
                label = if (selectedFilterCount > 0) selectedFilterCount.toString() else null,
            )
        }

        RunvealButton(
            label = "경로 추천 받기",
            onClick = onRecommendClick,
            style = RunvealButtonStyle.Gold,
            size = RunvealButtonSize.L,
            fillWidth = true,
        )
    }
}

@Preview
@Composable
private fun MakeCoursePanelPreview() {
    RunvealTheme {
        MakeCoursePanel(
            startLocation = "현위치",
            endLocation = "출발지로 돌아오기",
            distanceLabel = "5km",
            avoidVisitedPreferred = true,
            onStartLocationClick = {},
            onEndLocationClick = {},
            onDistanceClick = {},
            onAvoidVisitedPreferredChange = {},
            onFilterClick = {},
            onRecommendClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun RouteSettingRow(label: String, location: String, locationColor: Color, onClick: () -> Unit) {
    val colors = RunvealTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = RunvealTheme.spacing.space4, vertical = RunvealTheme.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = RunvealTheme.typography.bodyMRegular,
            color = colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            location,
            style = RunvealTheme.typography.labelButton,
            color = locationColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = RunvealIcons.Chevron,
            contentDescription = null,
            tint = colors.icon.tertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
