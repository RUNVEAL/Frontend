package com.sloth.runveal.core.map.internal.glow

import com.sloth.runveal.dong.glow.DongGlowSpec
import java.security.MessageDigest

/**
 * 생성 결과를 디스크에 캐시할 때 쓸 폴더 이름(문서 4장). [DongGlowSpec.VERSION]/`QUALITY`, 효과·테두리
 * 두께 토큰 값, 그리고 경계 출처(`boundarySourceKey`: `"server:<지도 버전>"`)가 하나라도 바뀌면 다른
 * 키가 나와 이전 캐시 폴더를 자연히 무효화한다(같은 키가 아니면 [DongGlowDiskCache.ensureCacheDir]가
 * 지운다).
 *
 * 출처가 키에 꼭 들어가야 한다: 서버가 동 경계를 고쳐 새 지도 버전으로 배포하면 테두리 선은 새
 * 경계로 바뀌는데, 빛 이미지는 옛 경계로 만든 캐시가 남아 둘이 어긋난 채로 보인다.
 */
internal object DongGlowCacheKey {
    private var cached: Pair<String, String>? = null

    fun compute(boundarySourceKey: String): String {
        cached?.let { (key, value) -> if (key == boundarySourceKey) return value }

        val digest = MessageDigest.getInstance("SHA-256")
        fun feed(text: String) = digest.update(text.toByteArray(Charsets.UTF_8))

        feed("v${DongGlowSpec.VERSION}|q${DongGlowSpec.QUALITY}|")
        DongGlowEffects.effectsByLevel.forEach { level ->
            level.forEach { e -> feed("${e.r},${e.g},${e.b},${e.alpha},${e.blur},${e.inset};") }
        }
        DongGlowEffects.strokeWidthPxByLevel.forEach { w -> feed("$w;") }
        feed("source=$boundarySourceKey")

        val hex = digest.digest().joinToString("") { "%02x".format(it) }.take(16)
        cached = boundarySourceKey to hex
        return hex
    }
}
