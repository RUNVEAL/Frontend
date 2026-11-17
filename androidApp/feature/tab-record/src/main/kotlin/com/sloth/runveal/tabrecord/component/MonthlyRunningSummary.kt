package com.sloth.runveal.tabrecord.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 기록 탭 TopAppBar 바로 아래, 이번 달 러닝 총 거리 + 새로 탐험한 길 요약(가운데 정렬). */
@Composable
fun MonthlyRunningSummary(
    runCountLabel: String,
    totalDistanceValue: String,
    totalDistanceUnit: String,
    newRoadLabel: String,
    newRoadValue: String,
    newRoadUnit: String,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        // 9dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("이번 달 러닝", style = RunvealTheme.typography.bodyLRegular, color = colors.text.secondary)
                Text(runCountLabel, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                Text(
                    totalDistanceValue,
                    style = RunvealTheme.typography.numDisplay,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    totalDistanceUnit,
                    style = RunvealTheme.typography.unitDisplay,
                    color = colors.text.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        Row(
            // 36dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(newRoadLabel, style = RunvealTheme.typography.bodyLRegular, color = colors.text.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
                Text(
                    newRoadValue,
                    style = RunvealTheme.typography.numM,
                    color = colors.text.accent,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    newRoadUnit,
                    style = RunvealTheme.typography.unitM,
                    color = colors.text.accent,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MonthlyRunningSummaryPreview() {
    RunvealTheme {
        MonthlyRunningSummary(
            runCountLabel = "8회",
            totalDistanceValue = "32.40",
            totalDistanceUnit = "km",
            newRoadLabel = "새로 탐험한 길",
            newRoadValue = "+6.20",
            newRoadUnit = "km",
        )
    }
}
