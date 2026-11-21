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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabrecord.R

/** 기록 탭 TopAppBar 바로 아래, 보고 있는 달의 총 거리 + 새로 탐험한 길 요약(가운데 정렬).
 * [periodLabel]은 "이번 달"처럼 어느 달인지 가리키는 앞머리다 — 달을 고를 수 있어서 늘 이번 달은 아니다. */
@Composable
fun MonthlyRunningSummary(
    periodLabel: String,
    runCountLabel: String,
    totalDistanceValue: String,
    totalDistanceUnit: String,
    newRoadLabel: String,
    newRoadValue: String,
    newRoadUnit: String,
    modifier: Modifier = Modifier,
    /** 이번 달에 처음 간 길이 있는지 — 0이면 시안처럼 금색 강조를 빼고 보조 글자색으로 둔다. */
    hasNewRoad: Boolean = true,
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
                Text(
                    stringResource(R.string.record_summary_run_count_format, periodLabel),
                    style = RunvealTheme.typography.bodyLRegular,
                    color = colors.text.secondary,
                )
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

        val newRoadColor = if (hasNewRoad) colors.text.accent else colors.text.secondary
        Row(
            // 36dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(newRoadLabel, style = RunvealTheme.typography.bodyLRegular, color = newRoadColor)
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
                Text(
                    newRoadValue,
                    style = RunvealTheme.typography.numM,
                    color = newRoadColor,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    newRoadUnit,
                    style = RunvealTheme.typography.unitM,
                    color = newRoadColor,
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
            periodLabel = "이번 달",
            runCountLabel = "8회",
            totalDistanceValue = "32.40",
            totalDistanceUnit = "km",
            newRoadLabel = "새로 탐험한 길",
            newRoadValue = "+6.20",
            newRoadUnit = "km",
        )
    }
}
