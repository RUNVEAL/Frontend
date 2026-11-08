package com.swyp.runveal.core.map.internal.glow

import android.graphics.Bitmap
import com.swyp.runveal.dong.glow.DongGlowRenderer

/**
 * [DongGlowRenderer.RenderResult.rgba](straight alpha, 채널 0..1 Float)를 Android [Bitmap]으로 바꾼다.
 * `Bitmap.setPixels`는 비예비곱(straight) alpha를 받는다(문서 3-2-7) — 그대로 넣는다.
 */
internal fun DongGlowRenderer.RenderResult.toBitmap(): Bitmap {
    val pixels = IntArray(width * height)
    for (i in pixels.indices) {
        val o = i * 4
        val r = toChannel(rgba[o])
        val g = toChannel(rgba[o + 1])
        val b = toChannel(rgba[o + 2])
        val a = toChannel(rgba[o + 3])
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun toChannel(value: Float): Int = (value * 255f + 0.5f).toInt().coerceIn(0, 255)
