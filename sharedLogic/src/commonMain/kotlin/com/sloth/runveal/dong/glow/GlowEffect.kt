package com.sloth.runveal.dong.glow

/**
 * `RunvealEffects.dong1`~`dong6`(디자인 토큰) 한 겹을 플랫폼 의존 없는 숫자로 옮긴 것. `color`는
 * `r/g/b`(0..1)와 `alpha`(0..1)로 나누고, `blur`는 dp가 아니라 **피그마 px 숫자 그대로**
 * (`RunvealShadow.blur.value`, 화면 밀도 미적용) 넣는다. `blendMode = Plus`(LINEAR_DODGE)인 레이어도
 * [DongGlowCompositor]는 항상 일반 합성으로 섞는다 — 토큰 쪽에 blendMode가 있어도 여기서는 뺀다
 * (`tools/dong-glow/README.md` 4번: 어두운 바탕에서는 차이가 거의 없다).
 */
data class GlowEffect(
    val r: Float,
    val g: Float,
    val b: Float,
    val alpha: Float,
    val blur: Float,
    val inset: Boolean,
)
