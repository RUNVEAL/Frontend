package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.designsystem.map.RunvealDong
import org.maplibre.android.style.expressions.Expression

/**
 * `level`(0~6) 숫자 속성에 대한 step 보간 색 Expression. 레벨은 순서가 있는 값이라 step이 타입
 * 민감한 match보다 안전하다. 동/시군구/시도 테두리가 전부 같은 [RunvealDong] 단계별 색 토큰을
 * 재사용해서([com.swyp.runveal.core.map.internal.maplibre.DongOverlayLayer],
 * [SigunguOverlayLayer], [SidoOverlayLayer]) 공용으로 뺐다 — 시군구·시도 전용 단계별 색 토큰은
 * 디자인시스템에 따로 없다.
 */
internal fun levelColorStep(colorForLevel: (Int) -> Int): Expression {
    val stops = (1..RunvealDong.MAX_LEVEL).map { level -> Expression.stop(level, Expression.color(colorForLevel(level))) }
    return Expression.step(Expression.get("level"), Expression.color(colorForLevel(0)), *stops.toTypedArray())
}
