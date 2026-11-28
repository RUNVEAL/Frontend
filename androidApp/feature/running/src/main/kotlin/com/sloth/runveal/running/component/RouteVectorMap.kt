package com.sloth.runveal.running.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.map.LatLon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/**
 * 달린 경로를 **지도 타일 없이 선으로만** 그린다. 저장 이미지(`ResultShareImage`)가 쓴다.
 *
 * 타일을 깔지 않는 이유: 저장은 네트워크가 없을 때도 돼야 하고, 타일을 쓰면 저작권 표기가 따라붙는다.
 * 어차피 이 자리에서 알아야 하는 건 "어떤 모양으로 달렸나"지 주변에 뭐가 있나가 아니다.
 *
 * 화면에 보이는 결과 카드([com.sloth.runveal.running.ResultScreen]의 `RouteMapCard`)는 그대로
 * 지도 스냅샷을 쓴다 — 거기서는 주변 지형이 보이는 편이 낫다.
 *
 * [newRoadPaths]는 [routePaths] 위에 덧그린다. 러닝 중 화면·결과 카드와 같은 순서라 "오늘 달린 길
 * 위에 처음 간 구간이 금색으로 얹히는" 모양이 같다.
 */
@Composable
fun RouteVectorMap(
    routePaths: List<List<LatLon>>,
    newRoadPaths: List<List<LatLon>>,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val todayColor = colors.map.roadToday
    val newColor = colors.map.roadNew
    val startColor = colors.map.location
    // 도착 점은 gold200 — 시맨틱 이름이 `roadExplored`지만 값이 시안의 도착 점 색과 같다.
    val endColor = colors.map.roadExplored
    val endpointStroke = colors.text.primary

    Canvas(modifier = modifier) {
        val projection = RouteProjection.of(routePaths + newRoadPaths, size) ?: return@Canvas

        routePaths.forEach { drawRoute(it, projection, todayColor) }
        newRoadPaths.forEach { drawRoute(it, projection, newColor) }

        // 출발은 파랑, 도착은 금색 — 둘 다 흰 테두리를 둘러 경로 선 위에서 구분된다.
        routePaths.firstOrNull()?.firstOrNull()?.let {
            drawEndpoint(projection.toOffset(it), startColor, endpointStroke)
        }
        routePaths.lastOrNull()?.lastOrNull()?.let {
            drawEndpoint(projection.toOffset(it), endColor, endpointStroke)
        }
    }
}

private fun DrawScope.drawRoute(path: List<LatLon>, projection: RouteProjection, color: Color) {
    // 점이 하나뿐인 조각은 선이 안 되지만 그 자리에 점은 찍어 준다 — 아예 빠지면 경로가 끊겨 보인다.
    if (path.size == 1) {
        drawCircle(color, radius = ROUTE_STROKE.toPx() / 2, center = projection.toOffset(path[0]))
        return
    }
    val drawPath = androidx.compose.ui.graphics.Path()
    path.forEachIndexed { index, point ->
        val offset = projection.toOffset(point)
        if (index == 0) drawPath.moveTo(offset.x, offset.y) else drawPath.lineTo(offset.x, offset.y)
    }
    drawPath(
        path = drawPath,
        color = color,
        style = Stroke(width = ROUTE_STROKE.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawEndpoint(center: Offset, fill: Color, stroke: Color) {
    drawCircle(stroke, radius = ENDPOINT_RADIUS.toPx() + ENDPOINT_STROKE.toPx(), center = center)
    drawCircle(fill, radius = ENDPOINT_RADIUS.toPx(), center = center)
}

/**
 * 위경도를 그림 안의 좌표로 옮기는 셈. 경로 전체가 들어가도록 **가로세로 같은 배율**로 줄이고
 * 가운데에 놓는다 — 배율을 따로 주면 경로 모양이 찌그러진다.
 *
 * 경도는 위도에 따라 실제 거리가 달라지므로(고위도일수록 좁다) [lonScale]로 보정한다. 한 번의 러닝이
 * 걸치는 범위에서는 이 정도면 충분하고, 제대로 된 메르카토르 투영까지 갈 필요가 없다.
 */
private class RouteProjection(
    private val minLat: Double,
    private val minLon: Double,
    private val lonScale: Double,
    private val scale: Double,
    private val offsetX: Double,
    private val offsetY: Double,
    private val height: Double,
) {
    fun toOffset(point: LatLon): Offset {
        val x = (point.lon - minLon) * lonScale * scale + offsetX
        // 위도는 위로 갈수록 커지는데 화면 y는 아래로 갈수록 커진다 — 뒤집는다.
        val y = height - ((point.lat - minLat) * scale + offsetY)
        return Offset(x.toFloat(), y.toFloat())
    }

    companion object {
        /** 점이 하나도 없으면 null — 그릴 게 없다는 뜻이라 호출부가 캔버스를 비워 둔다. */
        fun of(paths: List<List<LatLon>>, size: Size): RouteProjection? {
            val points = paths.flatten()
            if (points.isEmpty() || size.width <= 0f || size.height <= 0f) return null

            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLon = points.minOf { it.lon }
            val maxLon = points.maxOf { it.lon }
            val lonScale = cos((minLat + maxLat) / 2 * PI / 180).coerceAtLeast(MIN_LON_SCALE)

            // 선 굵기와 출발·도착 점이 가장자리에서 잘리지 않게 안쪽으로 물린다.
            val padding = PADDING_RATIO * min(size.width, size.height)
            val usableWidth = (size.width - padding * 2).coerceAtLeast(1f).toDouble()
            val usableHeight = (size.height - padding * 2).coerceAtLeast(1f).toDouble()

            val spanX = (maxLon - minLon) * lonScale
            val spanY = maxLat - minLat
            // 제자리에서 끝난 러닝처럼 범위가 0이면 배율을 못 구한다 — 가운데 한 점으로 그린다.
            val scale = if (spanX <= 0.0 && spanY <= 0.0) {
                0.0
            } else {
                min(
                    if (spanX > 0.0) usableWidth / spanX else Double.MAX_VALUE,
                    if (spanY > 0.0) usableHeight / spanY else Double.MAX_VALUE,
                )
            }

            return RouteProjection(
                minLat = minLat,
                minLon = minLon,
                lonScale = lonScale,
                scale = scale,
                offsetX = padding + (usableWidth - spanX * scale) / 2,
                offsetY = padding + (usableHeight - spanY * scale) / 2,
                height = size.height.toDouble(),
            )
        }
    }
}

private val ROUTE_STROKE = 4.dp
private val ENDPOINT_RADIUS = 6.dp
private val ENDPOINT_STROKE = 2.dp

/** 그림 짧은 변의 이 비율만큼 사방을 비운다 — 경로가 가장자리에 붙어 잘려 보이지 않게. */
private const val PADDING_RATIO = 0.08f

/** 경도 보정 하한 — 극지방에서 0으로 나누는 걸 막는다(실제로 그 위도에서 달릴 일은 없다). */
private const val MIN_LON_SCALE = 0.01
