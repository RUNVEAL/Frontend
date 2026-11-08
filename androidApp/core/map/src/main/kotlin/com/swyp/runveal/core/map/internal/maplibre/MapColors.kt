package com.swyp.runveal.core.map.internal.maplibre

/**
 * `RunvealTheme.colors`(Composable 컨텍스트 전용)에서 뽑아낸 색을 MapLibre용 ARGB `Int`로 미리
 * 바꿔 둔 값. Composable 컨텍스트가 없는 SDK 레이어 코드([VisitedRoadLayer], [DongOverlayLayer],
 * [SigunguOverlayLayer], [CurrentLocationLayer])에 전달해서 쓴다.
 */
internal data class MapColors(
    val visitedRoadGlow: Int,
    val visitedRoadCore: Int,
    val currentLocation: Int,
    val dongLabelPrimary: Int,
    val dongLabelSecondary: Int,
    val sigunguLabel: Int,
)
