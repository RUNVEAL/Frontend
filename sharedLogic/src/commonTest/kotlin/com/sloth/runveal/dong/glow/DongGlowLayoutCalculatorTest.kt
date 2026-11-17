package com.sloth.runveal.dong.glow

import kotlin.test.Test
import kotlin.test.assertTrue

class DongGlowLayoutCalculatorTest {

    @Test
    fun shrinksQualityWhenSideExceedsMaxSide() {
        // bbox가 아주 넓고 내접원은 1m라 축소 없이는 수백만 px가 나오는 극단적인 입력.
        val layout = DongGlowLayoutCalculator.compute(
            minX = 0.0, minY = 0.0, maxX = 2_000_000.0, maxY = 100.0,
            inradiusM = 1.0, maxBlurPx = 10f,
        )

        assertTrue(layout.quality < DongGlowSpec.QUALITY, "quality=${layout.quality} should shrink below ${DongGlowSpec.QUALITY}")
        assertTrue(layout.widthPx <= DongGlowSpec.MAX_SIDE, "widthPx=${layout.widthPx} should be <= MAX_SIDE")
        assertTrue(layout.heightPx <= DongGlowSpec.MAX_SIDE, "heightPx=${layout.heightPx} should be <= MAX_SIDE")
    }

    @Test
    fun keepsFullQualityWhenSideFitsWithinMaxSide() {
        val layout = DongGlowLayoutCalculator.compute(
            minX = 0.0, minY = 0.0, maxX = 100.0, maxY = 100.0,
            inradiusM = 50.0, maxBlurPx = 10f,
        )

        assertTrue(layout.quality == DongGlowSpec.QUALITY, "quality=${layout.quality} should stay at ${DongGlowSpec.QUALITY}")
    }
}
