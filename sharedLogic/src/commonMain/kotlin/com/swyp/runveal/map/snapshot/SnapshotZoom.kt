package com.swyp.runveal.map.snapshot

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin

private const val WORLD_TILE_PX = 256.0
private const val LN2 = 0.6931471805599453

/** [GeoBounds]를 (widthPx x heightPx) 뷰포트에 맞춰 보여줄 때 필요한 줌 레벨(Web Mercator "fit bounds"
 * 공식 — MapLibre/Mapbox GL의 `cameraForBounds`와 같은 식). `MapSnapshotter.Options.withRegion`은 자체
 * 최대 줌 제한이 없어서, 아주 작은 범위(최소 범위로 넓혀진 제자리 러닝 등)를 큰 픽셀로 스냅샷하면 줌이
 * 과도하게 깊어질 수 있다 — 호출 쪽이 이 값을 [SnapshotConfig.MAX_ZOOM]과 비교해 넘으면
 * `withCameraPosition`으로 바꿔 쓴다(`docs/running/COURSE_MAP_SNAPSHOT.md` §4 Step 2). */
fun GeoBounds.zoomToFit(widthPx: Int, heightPx: Int): Double {
    val latFraction = (latRad(north) - latRad(south)) / PI
    val lngDiffRaw = east - west
    val lngDiff = if (lngDiffRaw < 0) lngDiffRaw + 360.0 else lngDiffRaw
    val lngFraction = lngDiff / 360.0

    val latZoom = zoomFor(heightPx.toDouble(), latFraction)
    val lngZoom = zoomFor(widthPx.toDouble(), lngFraction)
    return min(latZoom, lngZoom)
}

private fun latRad(latDegrees: Double): Double {
    val sinLat = sin(latDegrees * PI / 180.0)
    val radX2 = ln((1 + sinLat) / (1 - sinLat)) / 2
    return radX2.coerceIn(-PI, PI) / 2
}

/** fraction이 0에 가까우면(범위가 거의 없으면) 로그가 발산하니 아주 작은 값으로 바닥을 둔다. */
private fun zoomFor(viewportPx: Double, fraction: Double): Double {
    val safeFraction = if (fraction <= 0.0) 1e-9 else fraction
    return ln(viewportPx / WORLD_TILE_PX / safeFraction) / LN2
}
