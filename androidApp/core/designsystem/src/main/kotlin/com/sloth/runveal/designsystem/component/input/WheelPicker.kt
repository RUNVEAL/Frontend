package com.sloth.runveal.designsystem.component.input

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val WheelRowHeight = 44.dp
private const val WheelVisibleRows = 5

/** 센터 기준 위아래로 더 그려두는 줄 수 — 드래그 중엔 인덱스를 바꾸지 않고 화면 밖 값까지 끊김 없이 보여준다. */
private const val WheelRowBuffer = 10

/**
 * 숫자 휠 선택(거리 등). 드래그하는 동안은 인덱스를 바꾸지 않고 손가락을 그대로 따라 계속 움직이다가,
 * 손을 뗐을 때 가장 가까운 값 하나로 확정하고 그 자리로 스냅 애니메이션한다. 보이는 줄을 바로 눌러도 고를 수 있다.
 */
@Composable
fun RunvealWheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val density = LocalDensity.current
    val rowPx = remember(density) { with(density) { WheelRowHeight.toPx() } }
    var offset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    // 드래그 중 실제로 센터 줄에 와 있는 값 — offset이 바뀔 때마다 다시 계산하되,
    // 반올림된 행이 실제로 바뀔 때만 리컴포지션을 일으킨다(인덱스 커밋과는 별개로 스타일만 따라간다).
    val centerRowOffset by remember {
        derivedStateOf { (-offset / rowPx).roundToInt().coerceIn(-WheelRowBuffer, WheelRowBuffer) }
    }

    Box(
        modifier = modifier
            .height(WheelRowHeight * WheelVisibleRows)
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(selectedIndex, values.size) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        val steps = (-offset / rowPx).roundToInt()
                        val newIndex = (selectedIndex + steps).coerceIn(0, values.lastIndex)
                        // 경계에 걸려 steps만큼 다 못 움직였으면 그만큼만 보정해서 시각 위치를 이어 붙인다.
                        val start = offset + (newIndex - selectedIndex) * rowPx
                        if (newIndex != selectedIndex) onSelectedIndexChange(newIndex)
                        scope.launch { Animatable(start).animateTo(0f, spring()) { offset = value } }
                    },
                    onDragCancel = {
                        val start = offset
                        scope.launch { Animatable(start).animateTo(0f, spring()) { offset = value } }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
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

        // 버퍼 행까지 합치면 바깥 Box(5줄 높이)보다 커서, 제약 그대로면 Center 정렬이 clamp되어 위쪽으로 쏠린다.
        // unbounded로 실제 크기를 재서 측정한 뒤에 중앙 정렬해야 한다.
        Column(modifier = Modifier.wrapContentHeight(unbounded = true).graphicsLayer { translationY = offset }) {
            for (rowOffset in -WheelRowBuffer..WheelRowBuffer) {
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
                        val distanceFromCenter = rowOffset - centerRowOffset
                        if (distanceFromCenter == 0) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(value, style = RunvealTheme.typography.numL, color = colors.text.primary, modifier = Modifier.alignByBaseline())
                                Text(
                                    unit,
                                    style = RunvealTheme.typography.unitL,
                                    color = colors.text.secondary,
                                    modifier = Modifier.padding(start = RunvealTheme.spacing.space4).alignByBaseline(),
                                )
                            }
                        } else if (abs(distanceFromCenter) <= WheelVisibleRows / 2) {
                            val alpha = if (abs(distanceFromCenter) == 1) 0.6f else 0.3f
                            Text(
                                value,
                                style = RunvealTheme.typography.numM,
                                color = colors.text.tertiary,
                                modifier = Modifier.graphicsLayer(alpha = alpha),
                            )
                        } else {
                            // 보이는 5줄 밖(드래그 중 화면 밖으로 넘어갈 수 있는 값)은 흐려진 숫자로만 채운다.
                            Text(
                                value,
                                style = RunvealTheme.typography.numM,
                                color = colors.text.tertiary,
                                modifier = Modifier.graphicsLayer(alpha = 0.15f),
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
