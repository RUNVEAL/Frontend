package com.swyp.runveal.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Figma 효과 한 겹. RunvealEffects(자동 생성)의 shadow 토큰이 이 타입의 리스트다.
 * blur는 Figma 값 그대로라 기기에서 눈으로 한 번 맞춰야 할 수 있다.
 */
@Immutable
data class RunvealShadow(
    val color: Color,
    val blur: Dp,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val spread: Dp = 0.dp,
    /** true면 안쪽 빛(inner shadow) */
    val inset: Boolean = false,
    /** Figma LINEAR_DODGE = [BlendMode.Plus] */
    val blendMode: BlendMode = BlendMode.SrcOver,
)

/**
 * 바깥 빛(inset = false)만 그린다. background보다 **앞에** 둔다.
 * `Modifier.runvealDropShadow(shape, RunvealEffects.glowGold).background(color, shape)`
 */
fun Modifier.runvealDropShadow(shape: Shape, layers: List<RunvealShadow>): Modifier =
    layers.filterNot { it.inset }.fold(this) { acc, layer ->
        acc.dropShadow(shape, layer.toComposeShadow())
    }

/**
 * 안쪽 빛(inset = true)만 그린다. background보다 **뒤에** 둔다.
 * `Modifier.background(color, shape).runvealInnerShadow(shape, RunvealEffects.dong3)`
 */
fun Modifier.runvealInnerShadow(shape: Shape, layers: List<RunvealShadow>): Modifier =
    layers.filter { it.inset }.fold(this) { acc, layer ->
        acc.innerShadow(shape, layer.toComposeShadow())
    }

private fun RunvealShadow.toComposeShadow() = Shadow(
    radius = blur,
    color = color,
    spread = spread,
    offset = DpOffset(offsetX, offsetY),
    blendMode = blendMode,
)

/**
 * 글래스 표면 (현재는 블러 없는 fallback 스타일: glassFallbackSurface + border.subtle 1dp).
 *
 * TODO: 뒤 배경 블러(RunvealEffects.glassBlur)는 Compose 기본 API로 불가. Haze 등 backdrop blur
 *  라이브러리 도입 시 API 31+ / iOS에서는 surface.default + border.default + blur로 교체.
 */
fun Modifier.runvealGlass(shape: Shape): Modifier =
    this
        .background(RunvealEffects.glassFallbackSurface, shape)
        .border(1.dp, DefaultRunvealColors.border.subtle, shape)
