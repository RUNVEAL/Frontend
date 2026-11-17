package com.sloth.runveal.core.map.internal.glow

import com.sloth.runveal.designsystem.RunvealEffects
import com.sloth.runveal.designsystem.RunvealLayout
import com.sloth.runveal.designsystem.theme.RunvealShadow
import com.sloth.runveal.dong.glow.GlowEffect

/**
 * `RunvealEffects.dong1`~`dong6`(빛)과 `RunvealLayout.mapDongLevelStrokeWidth0`~`6`(테두리 두께) 토큰을
 * [GlowEffect] 리스트/px 숫자로 바꾼 것. 값은 전부 토큰에서 읽으며 여기에 다시 적지 않는다 — 토큰이
 * 바뀌면 [effectsByLevel]/[strokeWidthPxByLevel]도 자동으로 따라간다. 인덱스 0은 0단계(빛 없음)용 빈 값.
 */
internal object DongGlowEffects {
    val effectsByLevel: List<List<GlowEffect>> = with(RunvealEffects) {
        listOf(emptyList(), dong1, dong2, dong3, dong4, dong5, dong6)
    }.map { shadows -> shadows.toGlowEffects() }

    val strokeWidthPxByLevel: List<Float> = with(RunvealLayout) {
        listOf(
            mapDongLevelStrokeWidth0, mapDongLevelStrokeWidth1, mapDongLevelStrokeWidth2,
            mapDongLevelStrokeWidth3, mapDongLevelStrokeWidth4, mapDongLevelStrokeWidth5,
            mapDongLevelStrokeWidth6,
        )
    }.map { it.value }

    private fun List<RunvealShadow>.toGlowEffects(): List<GlowEffect> = map { shadow ->
        GlowEffect(
            r = shadow.color.red,
            g = shadow.color.green,
            b = shadow.color.blue,
            alpha = shadow.color.alpha,
            // 문서 2장: blur는 dp가 아니라 피그마 px 숫자 그대로(화면 밀도 미적용).
            blur = shadow.blur.value,
            inset = shadow.inset,
        )
    }
}
