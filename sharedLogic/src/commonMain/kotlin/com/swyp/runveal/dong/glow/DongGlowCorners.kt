package com.swyp.runveal.dong.glow

import com.swyp.runveal.map.LatLon

/** [DongGlowLayout]의 네 꼭짓점을 [좌상, 우상, 우하, 좌하] 순서의 위경도로 돌려준다(문서 3-2-8). */
internal object DongGlowCorners {
    fun compute(layout: DongGlowLayout): List<LatLon> = layout.cornersMeters().map(Mercator::unproject)
}
