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
    /** 처음 간 길만 골라 [routeGeoJson] 위에 금색("새로 탐험한 길")으로 덧그릴 선. 새로 간 길이
     * 없으면 `null`이고, 그때는 경로 전체가 한 색이다. */
    val highlightGeoJson: String? = null,
    /** 러닝 기록이면 `true` — 경로를 "오늘 달린 길"(흰색)로 깔고 [highlightGeoJson]을 금색으로
     * 덧그린다. 코스(아직 달리지 않은 경로)는 `false`라 경로색 한 가지로만 그린다. */
    val isRunRoute: Boolean = false,
) {
    fun cacheKeyBase(): String = "${variant.name.lowercase()}_$ownerId"
}

object SnapshotConfig {
    const val PADDING_RATIO = 0.12
    const val MIN_SPAN_METERS = 300.0
    const val MAX_ZOOM = 16.0

    /** 스타일·선 디자인이 바뀌면 올려서 캐시를 무효화한다. 3 = 러닝 경로의 기본선을 "오늘 달린
     * 길"(흰색)로 바꾸고, 기록 썸네일에도 "새로 탐험한 길"(금색) 덧그리기를 적용한 버전. */
    const val STYLE_VERSION = 3

    const val ROUTE_SOURCE_ID = "snapshot-route-source"
    const val ROUTE_LAYER_ID = "snapshot-route-layer"
    const val HIGHLIGHT_SOURCE_ID = "snapshot-highlight-source"
    const val HIGHLIGHT_LAYER_ID = "snapshot-highlight-layer"
    const val ENDPOINT_SOURCE_ID = "snapshot-endpoint-source"
    const val ENDPOINT_LAYER_ID = "snapshot-endpoint-layer"
}
