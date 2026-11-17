package com.sloth.runveal.dong.glow

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DongGlowCompositorTest {

    @Test
    fun singleLayerOutputsItsOwnAlphaAndColor() {
        val alpha = floatArrayOf(0.6f, 0f, 1f)
        val layer = GlowLayerResult(r = 1f, g = 0.5f, b = 0.25f, alpha = alpha)

        val rgba = DongGlowCompositor.composite(listOf(layer), pixelCount = 3)

        for (i in 0 until 3) {
            val o = i * 4
            assertEquals(alpha[i], rgba[o + 3], "alpha at pixel $i")
            if (alpha[i] > 0f) {
                assertTrue(abs(1f - rgba[o]) < 0.0001f, "r at pixel $i was ${rgba[o]}")
                assertTrue(abs(0.5f - rgba[o + 1]) < 0.0001f, "g at pixel $i was ${rgba[o + 1]}")
                assertTrue(abs(0.25f - rgba[o + 2]) < 0.0001f, "b at pixel $i was ${rgba[o + 2]}")
            }
        }
    }
}
