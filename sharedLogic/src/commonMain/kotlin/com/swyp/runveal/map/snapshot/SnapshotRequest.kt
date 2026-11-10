package com.swyp.runveal.map.snapshot

import com.swyp.runveal.map.LatLon

/** `docs/running/COURSE_MAP_SNAPSHOT.md` §2 — 코스 목록/기록 목록 썸네일과 러닝 결과 카드. */
enum class SnapshotVariant { THUMBNAIL, RESULT_CARD }

data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double)

/** 지도 스냅샷 렌더링 요청 — 크기(px)·pixelRatio·테마는 플랫폼(Android `core:map` 등)이 캐시 키에 붙인다. */
data class SnapshotRequest(
    val ownerId: String,
    val variant: SnapshotVariant,
    val routeGeoJson: String,
    val bounds: GeoBounds,
    val start: LatLon?,
    val end: LatLon?,
) {
    fun cacheKeyBase(): String = "${variant.name.lowercase()}_$ownerId"
}

object SnapshotConfig {
    const val PADDING_RATIO = 0.12
    const val MIN_SPAN_METERS = 300.0
    const val MAX_ZOOM = 16.0

    /** 스타일·선 디자인이 바뀌면 올려서 캐시를 무효화한다. */
    const val STYLE_VERSION = 1

    const val ROUTE_SOURCE_ID = "snapshot-route-source"
    const val ROUTE_LAYER_ID = "snapshot-route-layer"
    const val ENDPOINT_SOURCE_ID = "snapshot-endpoint-source"
    const val ENDPOINT_LAYER_ID = "snapshot-endpoint-layer"
}
