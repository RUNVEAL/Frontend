package com.swyp.runveal.map.snapshot

import com.swyp.runveal.map.LatLon
import kotlin.test.Test
import kotlin.test.assertTrue

class SnapshotZoomTest {

    @Test
    fun largerViewportNeedsHigherZoomForSameBounds() {
        val bounds = listOf(LatLon(37.5665, 126.9780)).snapshotBounds() // 최소 범위(300m)로 넓혀진 bbox

        val small = bounds.zoomToFit(widthPx = 72, heightPx = 72)
        val large = bounds.zoomToFit(widthPx = 600, heightPx = 600)
        assertTrue(large > small)
    }

    @Test
    fun widerBoundsNeedsLowerZoomForSameViewport() {
        val tight = listOf(LatLon(37.0, 127.0), LatLon(37.01, 127.01)).snapshotBounds()
        val wide = listOf(LatLon(37.0, 127.0), LatLon(38.0, 128.0)).snapshotBounds()

        assertTrue(tight.zoomToFit(300, 300) > wide.zoomToFit(300, 300))
    }

    @Test
    fun minimumSpanBoundsAtLargeThumbnailSizeCanExceedMaxZoom() {
        // 제자리 러닝(300m 최소 범위)을 큰 결과 카드 크기(예: 1000x268px@3x)로 찍으면 MAX_ZOOM(16)을
        // 넘을 수 있다 — 그래서 §4 Step 2가 withCameraPosition로 바꿔 쓰라고 요구한다.
        val bounds = listOf(LatLon(37.5665, 126.9780)).snapshotBounds()
        val zoom = bounds.zoomToFit(widthPx = 1000, heightPx = 268)
        assertTrue(zoom > SnapshotConfig.MAX_ZOOM)
    }
}
