package com.swyp.runveal.core.map.internal.glow

import android.util.LruCache

/** 바이트 기준(문서 4장: 32MB로 시작) [LruCache]. 키는 "admCd2:level". */
internal class DongGlowMemoryCache(maxBytes: Int = 32 * 1024 * 1024) {
    private val cache = object : LruCache<String, DongGlowAsset>(maxBytes) {
        override fun sizeOf(key: String, value: DongGlowAsset): Int = value.bitmap.byteCount
    }

    fun get(admCd2: String, level: Int): DongGlowAsset? = cache.get(key(admCd2, level))

    fun put(admCd2: String, level: Int, asset: DongGlowAsset) {
        cache.put(key(admCd2, level), asset)
    }

    private fun key(admCd2: String, level: Int) = "$admCd2:$level"
}
