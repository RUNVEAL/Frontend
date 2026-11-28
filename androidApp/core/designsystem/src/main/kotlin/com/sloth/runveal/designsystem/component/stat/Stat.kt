package com.sloth.runveal.designsystem.component.stat

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
import androidx.compose.ui.unit.sp
import com.sloth.runveal.designsystem.theme.RunvealTheme

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
    /** true면 Small/Large에서도 숫자+단위를 text/accent로 강조한다(코스 카드의 새 길 수치 등). */
    accent: Boolean = false,
    /**
     * true면 Small에서 숫자+단위만 한 단계 작게 쓴다 — 라벨은 그대로다. 코스 카드가 수치 네 개를
     * 다 넣기에 좁을 때 카드가 쓴다([compactNum]).
     */
    compactValue: Boolean = false,
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
                numberColor = if (accent) colors.text.accent else colors.text.primary,
                unitColor = if (accent) colors.text.accent else colors.text.secondary,
                gap = RunvealTheme.spacing.space2,
            )
        }

        RunvealStatType.Small -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2),
        ) {
            if (label != null) {
                Text(
                    label,
                    style = RunvealTheme.typography.captionSRegular,
                    color = colors.text.secondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            StatValue(
                value = value,
                unit = unit,
                numberStyle = RunvealTheme.typography.numS.let { if (compactValue) it.compactNum else it },
                unitStyle = RunvealTheme.typography.unitS.let { if (compactValue) it.compactUnit else it },
                numberColor = if (accent) colors.text.accent else colors.text.primary,
                unitColor = if (accent) colors.text.accent else colors.text.secondary,
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

/**
 * num/s를 한 단계 줄인 값 스타일 — 12/14, 자간 -1%. 디자인 가이드의 "축소 폰트"로, 토큰에는
 * 대응하는 스타일이 없어서(num/s가 가장 작다) 여기서 파생시킨다.
 */
private val TextStyle.compactNum: TextStyle
    get() = copy(fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = (-0.12).sp)

/** unit/s를 한 단계 줄인 단위 스타일 — 10/12. [compactNum]과 짝이다. */
private val TextStyle.compactUnit: TextStyle
    get() = copy(fontSize = 10.sp, lineHeight = 12.sp)

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
        Text(
            value,
            style = numberStyle,
            color = numberColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.alignByBaseline(),
        )
        if (unit != null) {
            Text(
                unit,
                style = unitStyle,
                color = unitColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
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
