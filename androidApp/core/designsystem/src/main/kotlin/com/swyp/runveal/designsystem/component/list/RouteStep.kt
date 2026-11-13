package com.swyp.runveal.designsystem.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealDropShadow

enum class RunvealRouteStepType { Start, Waypoint, NewRoad, End }

private val RouteStepDotSize = 8.dp
private val RouteStepConnectorWidth = 1.dp
private val RouteStepConnectorHeight = 8.dp

/** 코스 상세 경로 정보 한 줄. 줄 사이 연결선은 [showConnector], 마지막 줄은 꺼서 끊는다. */
@Composable
fun RunvealRouteStep(
    place: String,
    distance: String,
    type: RunvealRouteStepType,
    modifier: Modifier = Modifier,
    note: String? = null,
    showConnector: Boolean = true,
) {
    val colors = RunvealTheme.colors

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteStepDot(type)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // place + note는 한 Row에 나란히 — note는 줄 아래가 아니라 place 옆에 온다.
                Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4), verticalAlignment = Alignment.Top) {
                    Text(place, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                    if (note != null) {
                        Text(note, style = RunvealTheme.typography.bodyMRegular, color = colors.text.tertiary)
                    }
                }
                Text(distance, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            }
        }
        if (showConnector) {
            Box(
                modifier = Modifier
                    .padding(start = (RouteStepDotSize - RouteStepConnectorWidth) / 2)
                    .size(width = RouteStepConnectorWidth, height = RouteStepConnectorHeight)
                    .background(colors.border.default),
            )
        }
    }
}

@Composable
private fun RouteStepDot(type: RunvealRouteStepType) {
    val colors = RunvealTheme.colors
    val shape = CircleShape
    when (type) {
        RunvealRouteStepType.Start -> Box(
            modifier = Modifier.size(RouteStepDotSize).clip(shape).background(colors.accent.glow, shape).border(1.dp, colors.icon.tertiary, shape),
        )
        RunvealRouteStepType.Waypoint -> Box(
            modifier = Modifier.size(RouteStepDotSize).clip(shape).border(1.dp, colors.icon.tertiary, shape),
        )
        RunvealRouteStepType.NewRoad -> Box(
            modifier = Modifier
                .size(RouteStepDotSize)
                .runvealDropShadow(shape, RunvealTheme.effects.glowGold)
                .clip(shape)
                .background(colors.accent.default, shape),
        )
        RunvealRouteStepType.End -> Box(
            modifier = Modifier.size(RouteStepDotSize).clip(shape).background(colors.icon.primary, shape).border(1.dp, colors.icon.tertiary, shape),
        )
    }
}

@Preview
@Composable
private fun RunvealRouteStepPreview() {
    RunvealTheme {
        Column {
            RunvealRouteStep(place = "성수역 2번 출구", distance = "0km", type = RunvealRouteStepType.Start)
            RunvealRouteStep(place = "서울숲", distance = "1.2km", type = RunvealRouteStepType.Waypoint)
            RunvealRouteStep(place = "뚝섬한강공원", distance = "3.5km", type = RunvealRouteStepType.NewRoad, note = "새로 탐험한 길")
            RunvealRouteStep(place = "성수역 2번 출구", distance = "5.21km", type = RunvealRouteStepType.End, showConnector = false)
        }
    }
}
