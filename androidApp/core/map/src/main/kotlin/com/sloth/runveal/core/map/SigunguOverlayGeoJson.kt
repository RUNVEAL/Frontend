package com.sloth.runveal.core.map

/** 시군구 경계 오버레이 — 줌아웃 시 보여줄 테두리/라벨 레이어용 GeoJSON `FeatureCollection` 문자열. */
data class SigunguOverlayGeoJson(
    val lineGeoJson: String,
    val labelGeoJson: String,
) {
    companion object {
        val EMPTY = SigunguOverlayGeoJson(EMPTY_FEATURE_COLLECTION, EMPTY_FEATURE_COLLECTION)
    }
}
