package com.swyp.runveal.core.map.internal.maplibre

/**
 * `RunvealTheme.colors`(Composable 컨텍스트 전용)에서 뽑아낸 색을 MapLibre용 ARGB `Int`로 미리
 * 바꿔 둔 값. Composable 컨텍스트가 없는 SDK 레이어 코드([VisitedRoadLayer], [DongOverlayLayer],
 * [SigunguOverlayLayer], [CurrentLocationLayer], [RecommendedRouteLayer])에 전달해서 쓴다.
 */
internal data class MapColors(
    val visitedRoadGlow: Int,
    val visitedRoadCore: Int,
    val currentLocation: Int,
    val dongLabelPrimary: Int,
    val dongLabelSecondary: Int,
    val sigunguLabel: Int,
    val recommendedRouteAlt: Int,
    val recommendedRouteSelectedGlow: Int,
    val recommendedRouteSelected: Int,
    val courseGuide: Int,
    /** 러닝 중 실시간 선 — 이미 가본 구간(흰색, glow 없음). */
    val liveRunOld: Int,
    /** 러닝 중 실시간 선 — 처음 가보는 구간(금색 코어 + glow). */
    val liveRunNew: Int,
    val liveRunNewGlow: Int,
    /** "안 가본 길" 보기에서 베이스 지도 도로를 덮는 색. */
    val roadUnexploredFocus: Int,
)
