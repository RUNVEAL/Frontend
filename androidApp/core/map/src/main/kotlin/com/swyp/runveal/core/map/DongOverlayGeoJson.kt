package com.swyp.runveal.core.map

/** 레이어를 비울 때 넣는 빈 GeoJSON — MapLibre 소스는 null을 못 받아서 빈 컬렉션을 준다. */
internal const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""

/** 동 완주율 오버레이 — 채우기/테두리/라벨 레이어 각각에 들어갈 GeoJSON `FeatureCollection` 문자열. */
data class DongOverlayGeoJson(
    val fillGeoJson: String,
    val lineGeoJson: String,
    val labelGeoJson: String,
) {
    companion object {
        /** 그릴 경계가 없을 때(서버 동 데이터를 못 받음) 쓰는 빈 오버레이. */
        val EMPTY = DongOverlayGeoJson(EMPTY_FEATURE_COLLECTION, EMPTY_FEATURE_COLLECTION, EMPTY_FEATURE_COLLECTION)
    }
}
