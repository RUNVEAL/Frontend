package com.swyp.runveal.designsystem.component.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.R
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealDropShadow

/** 현위치는 항상 같은 파란 점, 위계는 출발·도착 마커로 잡는다. Pin은 출발지·도착지 선택 모드에서만. */
enum class RunvealMapMarkerType { Current, Start, End, CurrentStart }

enum class RunvealMapMarkerStyle { Dot, Pin }

/** Small은 코스 썸네일 안 출발·도착 점. */
enum class RunvealMapMarkerSize { Default, Small }

/** 지도 마커. 지도 SDK 마커 이미지로 사용한다. 1차 `RunvealLocationMarker`(Type=Current, Style=Dot)를 대체한다. */
@Composable
fun RunvealMapMarker(
    type: RunvealMapMarkerType,
    modifier: Modifier = Modifier,
    style: RunvealMapMarkerStyle = RunvealMapMarkerStyle.Dot,
    size: RunvealMapMarkerSize = RunvealMapMarkerSize.Default,
    showLabel: Boolean = true,
    label: String? = null,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    when {
        type == RunvealMapMarkerType.Current -> Box(
            modifier = modifier
                .size(18.dp)
                .clip(shape)
                .background(colors.map.location, shape)
                .border(2.dp, colors.icon.primary, shape),
        )

        type == RunvealMapMarkerType.CurrentStart -> Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .background(colors.map.location.copy(alpha = 0.3f)),
            )
            DotMarker(fill = colors.accent.glow, borderColor = colors.icon.onAccent, dotSize = 18.dp, borderWidth = 2.dp)
        }

        style == RunvealMapMarkerStyle.Pin -> PinMarker(
            type = type,
            showLabel = showLabel,
            label = label,
            modifier = modifier,
        )

        else -> {
            val dotSize = if (size == RunvealMapMarkerSize.Default) 18.dp else 8.dp
            val borderWidth = if (size == RunvealMapMarkerSize.Default) 2.dp else 1.dp
            when (type) {
                RunvealMapMarkerType.Start -> DotMarker(
                    fill = colors.accent.glow,
                    borderColor = colors.icon.onAccent,
                    dotSize = dotSize,
                    borderWidth = borderWidth,
                    modifier = modifier,
                )
                RunvealMapMarkerType.End -> DotMarker(
                    fill = colors.icon.primary,
                    borderColor = colors.icon.onAccent,
                    dotSize = dotSize,
                    borderWidth = borderWidth,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun DotMarker(
    fill: Color,
    borderColor: Color,
    dotSize: Dp,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RunvealTheme.shapes.full
    Box(
        modifier = modifier
            .size(dotSize)
            .clip(shape)
            .background(fill, shape)
            .border(borderWidth, borderColor, shape),
    )
}

@Composable
private fun PinMarker(
    type: RunvealMapMarkerType,
    showLabel: Boolean,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val accentColor = if (type == RunvealMapMarkerType.Start) colors.accent.glow else colors.icon.primary
    val labelShape = RunvealTheme.shapes.radius8
    val dotShape = RunvealTheme.shapes.full
    val resolvedLabel = label ?: stringResource(
        if (type == RunvealMapMarkerType.Start) R.string.map_marker_start_label else R.string.map_marker_end_label,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (showLabel) {
            Text(
                resolvedLabel,
                style = RunvealTheme.typography.bodyMSemibold,
                color = colors.text.onAccent,
                modifier = Modifier
                    .clip(labelShape)
                    .background(accentColor, labelShape)
                    .padding(vertical = RunvealTheme.spacing.space2, horizontal = RunvealTheme.spacing.space8),
            )
        }
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 10.dp)
                .background(accentColor),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .runvealDropShadow(dotShape, RunvealTheme.effects.glowGoldStrong)
                .clip(dotShape)
                .background(accentColor, dotShape),
        )
    }
}

@Preview
@Composable
private fun RunvealMapMarkerPreview() {
    RunvealTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RunvealMapMarker(type = RunvealMapMarkerType.Current)
            RunvealMapMarker(type = RunvealMapMarkerType.CurrentStart)
            RunvealMapMarker(type = RunvealMapMarkerType.Start, size = RunvealMapMarkerSize.Small)
            RunvealMapMarker(type = RunvealMapMarkerType.End, size = RunvealMapMarkerSize.Small)
            RunvealMapMarker(type = RunvealMapMarkerType.Start, style = RunvealMapMarkerStyle.Pin)
            RunvealMapMarker(type = RunvealMapMarkerType.End, style = RunvealMapMarkerStyle.Pin)
        }
    }
}
