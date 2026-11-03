package com.swyp.runveal.designsystem.component.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme
import kotlin.math.abs

private val WheelRowHeight = 44.dp
private const val WheelVisibleRows = 5

/** 숫자 휠 선택(거리 등). 드래그로 한 칸씩 움직이거나 보이는 줄을 바로 눌러 고른다. */
@Composable
fun RunvealWheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val halfVisible = WheelVisibleRows / 2

    Box(
        modifier = modifier
            .height(WheelRowHeight * WheelVisibleRows)
            .fillMaxWidth()
            .pointerInput(selectedIndex, values.size) {
                var accumulated = 0f
                val rowPx = WheelRowHeight.toPx()
                detectVerticalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount
                        if (abs(accumulated) >= rowPx) {
                            val steps = (accumulated / rowPx).toInt()
                            val newIndex = (selectedIndex - steps).coerceIn(0, values.lastIndex)
                            if (newIndex != selectedIndex) onSelectedIndexChange(newIndex)
                            accumulated -= steps * rowPx
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(WheelRowHeight)
                .drawBehind {
                    val strokePx = 1.dp.toPx()
                    drawLine(colors.border.default, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = strokePx)
                    drawLine(colors.border.default, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = strokePx)
                },
        )

        Column {
            for (rowOffset in -halfVisible..halfVisible) {
                val index = selectedIndex + rowOffset
                val value = values.getOrNull(index)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelRowHeight)
                        .clickable(enabled = value != null) { onSelectedIndexChange(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (value != null) {
                        if (rowOffset == 0) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(value, style = RunvealTheme.typography.numL, color = colors.text.primary, modifier = Modifier.alignByBaseline())
                                Text(
                                    unit,
                                    style = RunvealTheme.typography.unitL,
                                    color = colors.text.secondary,
                                    modifier = Modifier.padding(start = RunvealTheme.spacing.space4).alignByBaseline(),
                                )
                            }
                        } else {
                            val alpha = if (abs(rowOffset) == 1) 0.6f else 0.3f
                            Text(
                                value,
                                style = RunvealTheme.typography.numM,
                                color = colors.text.tertiary,
                                modifier = Modifier.graphicsLayer(alpha = alpha),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RunvealWheelPickerPreview() {
    RunvealTheme {
        RunvealWheelPicker(
            values = (1..20).map { it.toString() },
            selectedIndex = 4,
            onSelectedIndexChange = {},
            unit = "km",
        )
    }
}
