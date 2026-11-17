package com.sloth.runveal.dong.glow

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GaussianBlurTest {

    @Test
    fun kernelSumsToOne() {
        for (sigma in listOf(0.5, 1.0, 3.0, 10.0)) {
            val kernel = GaussianBlur.kernel(sigma)
            val sum = kernel.sum()
            assertTrue(abs(sum - 1.0) < 1e-9, "sigma=$sigma sum=$sum")
        }
    }

    @Test
    fun zeroSigmaKeepsOriginal() {
        val src = floatArrayOf(0f, 1f, 0.5f, 0.25f, 0.75f, 1f)
        val result = GaussianBlur.blur(src, width = 3, height = 2, sigma = 0.0, padValue = 0f)

        assertEquals(src.toList(), result.toList())
    }
}
