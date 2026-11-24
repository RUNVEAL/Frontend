package com.sloth.runveal.ios

import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon

/**
 * Swift가 쓰는 거리 계산 — [GeoMath]를 그대로 감싼다. 계산식은 Android와 공유한다.
 *
 * 감싸는 이유: `GeoMath.distanceMeters`는 오버로드가 둘이라(좌표 4개 / [LatLon] 2개)
 * Kotlin/Native가 Swift 쪽 이름을 변형한다. 호출부가 그 변형된 이름에 기대지 않게 오버로드 없는
 * 함수 하나로 내보낸다.
 */
object IosGeo {

    /** 두 좌표 사이 **직선거리**(m). 길을 따라가는 거리가 아니다. */
    fun distanceMeters(from: LatLon, to: LatLon): Double = GeoMath.distanceMeters(from, to)
}
