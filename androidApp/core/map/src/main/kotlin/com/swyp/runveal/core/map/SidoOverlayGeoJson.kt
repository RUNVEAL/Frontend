package com.swyp.runveal.core.map

/** 시도(광역시·특별시·도) 경계 오버레이 — 가장 줌아웃했을 때 보여줄 테두리/라벨 레이어용 GeoJSON `FeatureCollection` 문자열. */
data class SidoOverlayGeoJson(
    val lineGeoJson: String,
    val labelGeoJson: String,
)
