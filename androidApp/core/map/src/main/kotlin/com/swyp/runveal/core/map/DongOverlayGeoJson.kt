package com.swyp.runveal.core.map

/** 동 완주율 오버레이 — 채우기/테두리/라벨 레이어 각각에 들어갈 GeoJSON `FeatureCollection` 문자열. */
data class DongOverlayGeoJson(
    val fillGeoJson: String,
    val lineGeoJson: String,
    val labelGeoJson: String,
)
