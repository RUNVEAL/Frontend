package com.sloth.runveal.map.snapshot

import com.sloth.runveal.map.LatLon

/**
 * `docs/running/COURSE_MAP_SNAPSHOT.md` §2 — 코스 목록/기록 목록 썸네일과 러닝 결과 카드.
 *
 * [DONG_THUMBNAIL]은 홈 요약 카드의 행정동 그림이다. 경로 한 줄이 아니라 **그 동 안의 가본 길
 * 전체**를 그리고, 출발·도착 마커가 없다 — 그래서 같은 렌더러(캐시·중복 제거·MapSnapshotter)를
 * 쓰면서 그릴 레이어만 다르게 간다.
 */
enum class SnapshotVariant { THUMBNAIL, RESULT_CARD, DONG_THUMBNAIL }

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
    /**
     * 동 썸네일에서 **동 밖을 덮을** 폴리곤([DongThumbnailGeometry.maskGeoJson]) — 주면 그 동 모양
     * 으로 잘린 것처럼 보인다. 다른 variant는 null이다.
     */
    val dongMaskGeoJson: String? = null,
    /** 동 썸네일에 얹을 경계선([DongThumbnailGeometry.outlineGeoJson]). 주지 않으면 안 그린다. */
    val dongOutlineGeoJson: String? = null,
) {
    fun cacheKeyBase(): String = "${variant.name.lowercase()}_$ownerId"
}

object SnapshotConfig {
    const val PADDING_RATIO = 0.12
    const val MIN_SPAN_METERS = 300.0
    const val MAX_ZOOM = 16.0

    /** 스타일·선 디자인이 바뀌면 올려서 캐시를 무효화한다. 4 = 코스 썸네일에 출발지 점을 그리기
     * 시작하고(그 전엔 카드가 썸네일 **한가운데** 점을 얹고 있었다) 경로 선을 3dp → 4dp로 굵힌 버전.
     * 5 = 그 출발지 점을 지름 기준 2.5배로 키우고 도착지 점까지 그리는 버전. */
    const val STYLE_VERSION = 5

    const val ROUTE_SOURCE_ID = "snapshot-route-source"
    const val ROUTE_LAYER_ID = "snapshot-route-layer"
    const val HIGHLIGHT_SOURCE_ID = "snapshot-highlight-source"
    const val HIGHLIGHT_LAYER_ID = "snapshot-highlight-layer"
    const val ENDPOINT_SOURCE_ID = "snapshot-endpoint-source"
    const val ENDPOINT_LAYER_ID = "snapshot-endpoint-layer"
    const val START_SOURCE_ID = "snapshot-start-source"
    const val START_LAYER_ID = "snapshot-start-layer"
    const val END_SOURCE_ID = "snapshot-end-source"
    const val END_LAYER_ID = "snapshot-end-layer"

    const val DONG_MASK_SOURCE_ID = "snapshot-dong-mask-source"
    const val DONG_MASK_LAYER_ID = "snapshot-dong-mask-layer"
    const val DONG_OUTLINE_SOURCE_ID = "snapshot-dong-outline-source"
    const val DONG_OUTLINE_LAYER_ID = "snapshot-dong-outline-layer"

    /**
     * 동 썸네일의 가본 길 두께(dp). 84dp 프레임에 동 하나가 다 들어가 길이 아주 얇아지므로
     * 코스 경로([ROUTE_WIDTH_DP]와 다른 값)보다 가늘게 둔다 — 굵으면 길이 뭉쳐 덩어리로 보인다.
     */
    const val DONG_ROAD_WIDTH_DP = 1.5

    /** 동 경계선 두께(dp) — 작은 프레임이라 가늘게. */
    const val DONG_OUTLINE_WIDTH_DP = 1.0
}
