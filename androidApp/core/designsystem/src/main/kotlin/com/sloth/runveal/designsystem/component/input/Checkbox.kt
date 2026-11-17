package com.sloth.runveal.designsystem.component.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme

/**
 * 여러 개를 각각 켜고 끄는 선택 표시(약관 동의 등). 클릭 핸들러가 없다 — 단독으로 두지 말고
 * List Item(leading=checkbox) 안에서 줄 전체를 누르게 한다.
 */
enum class RunvealCheckboxState { Unchecked, Mixed, Checked }

@Composable
fun RunvealCheckbox(state: RunvealCheckboxState, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .size(20.dp)
            .clip(shape)
            .then(
                if (state == RunvealCheckboxState.Unchecked) {
                    Modifier.border(1.5.dp, colors.border.strong, shape)
                } else {
                    Modifier.background(colors.accent.default, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            RunvealCheckboxState.Checked -> Canvas(modifier = Modifier.size(width = 10.dp, height = 8.dp)) {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.55f)
                    lineTo(size.width * 0.38f, size.height)
                    lineTo(size.width, 0f)
                }
                drawPath(
                    path = path,
                    color = colors.icon.onAccent,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            RunvealCheckboxState.Mixed -> Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 2.dp)
                    .background(colors.icon.onAccent),
            )

            RunvealCheckboxState.Unchecked -> Unit
        }
    }
}

@Preview
@Composable
private fun RunvealCheckboxPreview() {
    RunvealTheme {
        Row {
            RunvealCheckbox(state = RunvealCheckboxState.Unchecked)
            RunvealCheckbox(state = RunvealCheckboxState.Mixed)
            RunvealCheckbox(state = RunvealCheckboxState.Checked)
        }
    }
}
