package com.swyp.runveal.dong

import com.swyp.runveal.map.LatLon

/**
 * 행정동 하나의 경계(외곽 링들). MultiPolygon이면 ring이 여러 개다.
 * 폴리곤의 구멍(hole)은 무시한다(행정동 경계에 구멍이 있는 경우는 드물고, 점-포함 판정에 영향이 작다).
 */
data class DongBoundary(
    val admCd2: String,
    val name: String,
    val rings: List<List<LatLon>>,
)

/** 폴리곤 한 조각(exterior ring + 그 안의 구멍 ring들). MultiPolygon인 동은 이게 여러 개 있다. */
data class DongPolygonPart(
    val exterior: List<LatLon>,
    val holes: List<List<LatLon>>,
)
