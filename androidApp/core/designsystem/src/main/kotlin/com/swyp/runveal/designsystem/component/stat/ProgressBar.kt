package com.swyp.runveal.designsystem.component.stat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealDropShadow

/**
 * 탐험률 진행 바. [filledPercent]=기존 탐험률, [gainedPercent]=이번 러닝으로 늘어난 만큼
 * (0이면 숨김). gained는 filled 끝에서 4dp 겹쳐 시작하고, 보이는 길이는 최소 8dp
 * (2%처럼 작아도 보이게).
 */
@Composable
fun RunvealProgressBar(
    filledPercent: Float,
    gainedPercent: Float,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(RunvealTheme.layout.sizeProgressBar)
            .clip(shape)
            .background(colors.progress.track, shape),
    ) {
        val filledWidth = maxWidth * (filledPercent.coerceIn(0f, 100f) / 100f)

        Box(
            Modifier
                .width(filledWidth)
                .fillMaxHeight()
                .clip(shape)
                .background(colors.progress.filled, shape),
        )

        if (gainedPercent > 0f) {
            val gainedWidth = maxOf(maxWidth * (gainedPercent.coerceIn(0f, 100f) / 100f), 8.dp)
            val startX = (filledWidth - 4.dp).coerceAtLeast(0.dp)
            Box(
                Modifier
                    .offset(x = startX)
                    .width(gainedWidth)
                    .fillMaxHeight()
                    .clip(shape)
                    .runvealDropShadow(shape, RunvealTheme.effects.glowGold)
                    .background(colors.progress.gained, shape),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealProgressBarPreview() {
    RunvealTheme {
        RunvealProgressBar(filledPercent = 35f, gainedPercent = 2f)
    }
}
