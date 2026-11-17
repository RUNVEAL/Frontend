package com.sloth.runveal.courserecommend.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.courserecommend.contract.ElevationPointUiModel
import com.sloth.runveal.designsystem.theme.RunvealTheme
import kotlin.math.roundToInt

private val CHART_HEIGHT = 80.dp
private val LEGEND_WIDTH = 30.dp

/** 고도 프로필 그래프. 디자인시스템엔 이 차트 전용 컴포넌트가 없어 `RouteDto.elevationProfile`
 * 좌표로 여기서 직접 그린다 — Y축 레이블(최고/중간/최저)은 프로필 값에서 그대로 계산한다. */
@Composable
fun ElevationChart(points: List<ElevationPointUiModel>, modifier: Modifier = Modifier) {
    if (points.size < 2) return

    val colors = RunvealTheme.colors
    val minElevation = points.minOf { it.elevationM }
    val maxElevation = points.maxOf { it.elevationM }
    val midElevation = (minElevation + maxElevation) / 2
    val maxDistance = points.last().cumulativeDistanceM.coerceAtLeast(1.0)

    Row(
        modifier = modifier.fillMaxWidth().height(CHART_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Column(
            modifier = Modifier.width(LEGEND_WIDTH).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${maxElevation.roundToInt()}m", style = RunvealTheme.typography.unitS, color = colors.text.tertiary)
            Text("${midElevation.roundToInt()}m", style = RunvealTheme.typography.unitS, color = colors.text.tertiary)
            Text("${minElevation.roundToInt()}m", style = RunvealTheme.typography.unitS, color = colors.text.tertiary)
        }
        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val elevationRange = (maxElevation - minElevation).coerceAtLeast(1.0)
            fun offsetFor(point: ElevationPointUiModel): Offset {
                val x = (point.cumulativeDistanceM / maxDistance).toFloat() * size.width
                val y = size.height - ((point.elevationM - minElevation) / elevationRange).toFloat() * size.height
                return Offset(x, y)
            }

            val linePath = Path().apply {
                points.forEachIndexed { index, point ->
                    val offset = offsetFor(point)
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(fillPath, brush = Brush.verticalGradient(listOf(colors.accent.default.copy(alpha = 0.3f), colors.accent.default.copy(alpha = 0f))))
            drawPath(linePath, color = colors.accent.default, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Preview
@Composable
private fun ElevationChartPreview() {
    RunvealTheme {
        ElevationChart(
            points = listOf(
                ElevationPointUiModel(0.0, 20.0),
                ElevationPointUiModel(500.0, 45.0),
                ElevationPointUiModel(1200.0, 30.0),
                ElevationPointUiModel(2000.0, 60.0),
                ElevationPointUiModel(2870.0, 25.0),
            ),
        )
    }
}
