package com.sloth.runveal.dong.glow

/** 효과 레이어 하나의 결과: 단색 `r/g/b` + 픽셀별 알파. */
internal data class GlowLayerResult(val r: Float, val g: Float, val b: Float, val alpha: FloatArray)

/**
 * 레이어들을 premultiplied 누적으로 한 장에 합친다(문서 3-2-7). `blendMode = Plus`인 레이어도 항상
 * 일반 합성(source-over)으로 섞는다. 출력은 straight alpha RGBA, 각 채널 0..1.
 */
internal object DongGlowCompositor {
    /** [pixelCount]개 픽셀 각각에 대해 `rgba[i*4 + 0..3]`(r,g,b,a, 0..1)을 채운 [FloatArray]를 돌려준다. */
    fun composite(layers: List<GlowLayerResult>, pixelCount: Int): FloatArray {
        val out = FloatArray(pixelCount * 4)
        for (idx in 0 until pixelCount) {
            var premR = 0.0
            var premG = 0.0
            var premB = 0.0
            var a = 0.0
            for (layer in layers) {
                val layerA = layer.alpha[idx].toDouble().coerceIn(0.0, 1.0)
                val inv = 1.0 - layerA
                premR = premR * inv + layer.r * layerA
                premG = premG * inv + layer.g * layerA
                premB = premB * inv + layer.b * layerA
                a = a * inv + layerA
            }
            val o = idx * 4
            if (a > 0.0) {
                out[o] = (premR / a).toFloat()
                out[o + 1] = (premG / a).toFloat()
                out[o + 2] = (premB / a).toFloat()
            }
            out[o + 3] = a.toFloat()
        }
        return out
    }
}
