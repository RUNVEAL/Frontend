package com.swyp.runveal.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** 동 하나를 그릴 때 필요한 스타일 묶음. */
@Immutable
data class DongLevelStyle(
    val level: Int,
    val stroke: Color,
    val fill: Color,
    val strokeWidth: Dp,
    val glow: List<RunvealShadow>,
)

/**
 * 동 탐험률 단계. 값은 모두 자동 생성 토큰(RunvealColors.map.dong, RunvealLayout, RunvealEffects)에서 가져온다.
 * 밝기는 탐험률만 뜻한다. 내 동네는 단계 스타일 그대로, 테두리만 mapDongLevelMineStrokeWidth.
 *
 * | 단계 | 범위 |
 * |---|---|
 * | 0 | 0% (미탐험) |
 * | 1 | 1~4% |
 * | 2 | 5~9% |
 * | 3 | 10~19% |
 * | 4 | 20~39% |
 * | 5 | 40~69% |
 * | 6 | 70~100% |
 */
object RunvealDong {
    const val MAX_LEVEL = 6

    /** 정수 탐험률(%)을 단계(0..6)로 변환 */
    fun levelOf(ratePercent: Int): Int = when {
        ratePercent <= 0 -> 0
        ratePercent < 5 -> 1
        ratePercent < 10 -> 2
        ratePercent < 20 -> 3
        ratePercent < 40 -> 4
        ratePercent < 70 -> 5
        else -> 6
    }

    fun style(level: Int, colors: RunvealColors = DefaultRunvealColors, isMine: Boolean = false): DongLevelStyle {
        val l = level.coerceIn(0, MAX_LEVEL)
        val dong = colors.map.dong
        val stroke = listOf(dong.stroke0, dong.stroke1, dong.stroke2, dong.stroke3, dong.stroke4, dong.stroke5, dong.stroke6)
        val fill = listOf(Color.Transparent, dong.fill1, dong.fill2, dong.fill3, dong.fill4, dong.fill5, dong.fill6)
        val width = with(RunvealLayout) {
            listOf(
                mapDongLevelStrokeWidth0, mapDongLevelStrokeWidth1, mapDongLevelStrokeWidth2,
                mapDongLevelStrokeWidth3, mapDongLevelStrokeWidth4, mapDongLevelStrokeWidth5,
                mapDongLevelStrokeWidth6,
            )
        }
        val glow = with(RunvealEffects) { listOf(emptyList(), dong1, dong2, dong3, dong4, dong5, dong6) }
        return DongLevelStyle(
            level = l,
            stroke = stroke[l],
            fill = fill[l],
            strokeWidth = if (isMine) RunvealLayout.mapDongLevelMineStrokeWidth else width[l],
            glow = glow[l],
        )
    }
}
