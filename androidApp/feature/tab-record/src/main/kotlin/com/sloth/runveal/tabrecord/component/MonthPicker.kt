package com.sloth.runveal.tabrecord.component

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabrecord.R

/** 시안 값 — 패널 폭, 연도 줄 높이, 달 한 칸 크기. */
private val PICKER_WIDTH = 254.dp
private val YEAR_ROW_HEIGHT = 32.dp
private val MONTH_CELL_WIDTH = 52.dp
private val MONTH_CELL_HEIGHT = 36.dp
private const val MONTHS_PER_ROW = 4

/**
 * 월 선택 패널(디자인 `Overlay/Month Picker`) — 연도 한 줄 + 12달 격자(4칸 x 3줄).
 *
 * 연도는 패널이 직접 들고 넘긴다(화살표로 넘겨 봐도 고르기 전까지는 목록이 안 바뀐다) — 고른 달만
 * [onSelect]로 올린다. 띄우고 닫는 건 호출부가 맡는다(`RunvealMenu`와 같은 규칙).
 */
@Composable
internal fun MonthPicker(
    selectedYear: Int,
    selectedMonth: Int,
    onSelect: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    var shownYear by remember(selectedYear) { mutableIntStateOf(selectedYear) }

    Column(
        modifier = modifier
            .width(PICKER_WIDTH)
            .clip(shape)
            .background(colors.surface.raised, shape)
            .border(1.dp, colors.border.default, shape)
            .padding(RunvealTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(YEAR_ROW_HEIGHT),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YearArrow(
                contentDescription = stringResource(R.string.record_month_picker_previous_year),
                pointsLeft = true,
                onClick = { shownYear-- },
            )
            Text(
                stringResource(R.string.record_month_picker_year_format, shownYear),
                style = RunvealTheme.typography.bodyMSemibold,
                color = colors.text.primary,
            )
            YearArrow(
                contentDescription = stringResource(R.string.record_month_picker_next_year),
                pointsLeft = false,
                onClick = { shownYear++ },
            )
        }

        (1..12).chunked(MONTHS_PER_ROW).forEach { months ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                months.forEach { month ->
                    MonthCell(
                        month = month,
                        selected = shownYear == selectedYear && month == selectedMonth,
                        onClick = { onSelect(shownYear, month) },
                    )
                }
            }
        }
    }
}

/** 고른 달만 채워 그린다(밝은 면 + 어두운 글자). 나머지는 테두리만. */
@Composable
private fun MonthCell(month: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    // 고른 달이든 아니든 칸 모양은 같다(시안) — 고른 칸만 안을 채운다.
    val shape = RunvealTheme.shapes.radius8
    Box(
        modifier = modifier
            .width(MONTH_CELL_WIDTH)
            .height(MONTH_CELL_HEIGHT)
            .clip(shape)
            .then(if (selected) Modifier.background(colors.surface.inverse, shape) else Modifier)
            .border(1.dp, colors.border.default, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.record_month_picker_month_format, month),
            style = RunvealTheme.typography.bodyMSemibold,
            color = if (selected) colors.text.onInverse else colors.text.primary,
            textAlign = TextAlign.Center,
        )
    }
}

/** 연도를 넘기는 화살표. chevron 아이콘 하나를 좌우로 돌려 쓴다(왼쪽용 아이콘이 따로 없다). */
@Composable
private fun YearArrow(contentDescription: String, pointsLeft: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Icon(
        imageVector = RunvealIcons.Chevron,
        contentDescription = contentDescription,
        tint = RunvealTheme.colors.icon.primary,
        modifier = modifier
            .size(RunvealTheme.layout.sizeIcon)
            .rotate(if (pointsLeft) 180f else 0f)
            .clickable(onClick = onClick),
    )
}

@Preview
@Composable
private fun MonthPickerPreview() {
    RunvealTheme {
        MonthPicker(selectedYear = 2026, selectedMonth = 11, onSelect = { _, _ -> })
    }
}
