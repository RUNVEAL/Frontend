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
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 러닝 결과 수치. 시간처럼 단위가 없는 값은 [unit]을 생략. Highlight=결과 카드의 강조 한 줄. */
enum class RunvealStatType { Default, Highlight }

@Composable
fun RunvealStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    type: RunvealStatType = RunvealStatType.Default,
) {
    val colors = RunvealTheme.colors
    when (type) {
        RunvealStatType.Default -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            StatValue(
                value = value,
                unit = unit,
                numberStyle = RunvealTheme.typography.numM,
                unitStyle = RunvealTheme.typography.unitM,
                numberColor = colors.text.primary,
                unitColor = colors.text.secondary,
            )
        }

        RunvealStatType.Highlight -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.accent)
            StatValue(
                value = value,
                unit = unit,
                numberStyle = RunvealTheme.typography.numL,
                unitStyle = RunvealTheme.typography.unitL,
                numberColor = colors.text.accent,
                unitColor = colors.text.accent,
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
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2),
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
            RunvealStat(label = "거리", value = "5.21", unit = "km")
            RunvealStat(label = "새로 탐험한 길", value = "+1.3", unit = "km", type = RunvealStatType.Highlight)
        }
    }
}
