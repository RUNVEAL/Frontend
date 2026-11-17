package com.sloth.runveal.dong.glow

import kotlin.math.ceil
import kotlin.math.exp

/**
 * 분리형(가로→세로) 가우시안 블러. 커널 반지름은 `ceil(4σ)`, 합은 1로 정규화한다(문서 3-2-6).
 * 이미지 바깥은 [padValue]로 채워진 무한 평면으로 취급한다 — 바깥 빛(`padValue=0`)과 안쪽 빛
 * (`padValue=1`, 동 바깥으로 취급)이 가장자리에서 값이 달라야 해서 가장 가까운 픽셀을 복제하는 대신
 * 상수로 패딩한다.
 */
internal object GaussianBlur {
    fun blur(src: FloatArray, width: Int, height: Int, sigma: Double, padValue: Float): FloatArray {
        if (sigma <= 0.0) return src.copyOf()
        val kernel = kernel(sigma)
        val radius = kernel.size / 2

        val horizontal = FloatArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                var acc = 0.0
                for (i in kernel.indices) {
                    val sx = x + i - radius
                    val v = if (sx in 0 until width) src[rowOffset + sx] else padValue
                    acc += v * kernel[i]
                }
                horizontal[rowOffset + x] = acc.toFloat()
            }
        }

        val out = FloatArray(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var acc = 0.0
                for (i in kernel.indices) {
                    val sy = y + i - radius
                    val v = if (sy in 0 until height) horizontal[sy * width + x] else padValue
                    acc += v * kernel[i]
                }
                out[y * width + x] = acc.toFloat()
            }
        }
        return out
    }

    fun kernel(sigma: Double): DoubleArray {
        val radius = ceil(4 * sigma).toInt().coerceAtLeast(0)
        if (radius == 0) return doubleArrayOf(1.0)
        val size = 2 * radius + 1
        val values = DoubleArray(size)
        var sum = 0.0
        for (i in 0 until size) {
            val x = (i - radius).toDouble()
            val v = exp(-(x * x) / (2.0 * sigma * sigma))
            values[i] = v
            sum += v
        }
        for (i in values.indices) values[i] /= sum
        return values
    }
}
