package com.swyp.runveal.designsystem.component.stat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 러닝 결과·코스 수치. Display=기록 상세 맨 위 큰 거리, Large=결과 화면, Small=코스 카드 안 작은 수치,
 * Highlight=결과 카드의 새로 탐험한 길 한 줄. 시간처럼 단위가 없는 값은 [unit]을 생략, 라벨이 없으면
 * [label]을 생략(Display는 라벨이 아예 없다).
 */
enum class RunvealStatType { Display, Large, Small, Highlight }

@Composable
fun RunvealStat(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    unit: String? = null,
    type: RunvealStatType = RunvealStatType.Large,
) {
    val colors = RunvealTheme.colors
    when (type) {
        RunvealStatType.Display -> StatValue(
            value = value,
            unit = unit,
            numberStyle = RunvealTheme.typography.numDisplay,
            unitStyle = RunvealTheme.typography.unitDisplay,
            numberColor = colors.text.primary,
            unitColor = colors.text.secondary,
            gap = RunvealTheme.spacing.space4,
            modifier = modifier,
        )

        RunvealStatType.Large -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            if (label != null) {
                Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            }
            StatValue(
                value = value,
                unit = unit,
                numberStyle = RunvealTheme.typography.numM,
                unitStyle = RunvealTheme.typography.unitM,
                numberColor = colors.text.primary,
                unitColor = colors.text.secondary,
                gap = RunvealTheme.spacing.space2,
            )
        }

        RunvealStatType.Small -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2),
        ) {
            if (label != null) {
                Text(label, style = RunvealTheme.typography.captionSRegular, color = colors.text.secondary)
            }
            StatValue(
                value = value,
                unit = unit,
                numberStyle = RunvealTheme.typography.numS,
                unitStyle = RunvealTheme.typography.unitS,
                numberColor = colors.text.primary,
                unitColor = colors.text.secondary,
                gap = 0.dp,
            )
        }

        RunvealStatType.Highlight -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label != null) {
                Text(label, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.accent)
            }
            StatValue(
                value = value,
                unit = unit,
                numberStyle = RunvealTheme.typography.numL,
                unitStyle = RunvealTheme.typography.unitL,
                numberColor = colors.text.accent,
                unitColor = colors.text.accent,
                gap = RunvealTheme.spacing.space2,
            )
        }
    }
}

@Composable
private fun StatValue(
    value: String,
    unit: String?,
    numberStyle: TextStyle,
    unitStyle: TextStyle,
    numberColor: Color,
    unitColor: Color,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(value, style = numberStyle, color = numberColor, modifier = Modifier.alignByBaseline())
        if (unit != null) {
            Text(unit, style = unitStyle, color = unitColor, modifier = Modifier.alignByBaseline())
        }
    }
}

@Preview
@Composable
private fun RunvealStatPreview() {
    RunvealTheme {
        Column {
            RunvealStat(value = "5.21", unit = "km", type = RunvealStatType.Display)
            RunvealStat(label = "거리", value = "5.21", unit = "km")
            RunvealStat(value = "5.21", unit = "km", type = RunvealStatType.Small)
            RunvealStat(label = "새로 탐험한 길", value = "+1.3", unit = "km", type = RunvealStatType.Highlight)
        }
    }
}
