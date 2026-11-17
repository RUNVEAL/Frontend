package com.sloth.runveal.core.map.internal.snapshot

import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.map.snapshot.SnapshotConfig
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** 앱 지도와 같은 스타일([MapConfig.STYLE_URL]) 위에 경로 선 + (RESULT_CARD면) 시작·끝 점 레이어를
 * 얹는다. [VisitedRoadLayer]와 같은 패턴(`PropertyFactory` + 둥근 cap/join)이지만, 스냅샷은 줌 보간이
 * 필요 없는 고정 프레임 1장이라 두께를 상수로 바로 쓴다. */
internal object SnapshotStyleBuilder {
    private const val ROUTE_WIDTH_DP = 3f
    private const val ENDPOINT_RADIUS_DP = 5f
    private const val ENDPOINT_STROKE_WIDTH_DP = 2f

    fun build(
        request: SnapshotRequest,
        routeColor: Int,
        endpointColor: Int,
        endpointStrokeColor: Int,
        newRoadColor: Int,
        todayRoadColor: Int,
    ): Style.Builder {
        // 러닝 기록은 "오늘 달린 길"(흰색) 위에 "새로 탐험한 길"(금색)을 덧그려 둘을 구분한다 —
        // 결과 화면 범례가 그 두 색으로 설명하고 있어서 기본선도 범례와 같아야 한다. 아직 달리지
        // 않은 코스는 그 구분이 없어 경로색(금색) 한 가지로 그린다.
        val baseColor = if (request.isRunRoute) todayRoadColor else routeColor
        val builder = Style.Builder()
            .fromUri(MapConfig.STYLE_URL)
            .withSource(GeoJsonSource(SnapshotConfig.ROUTE_SOURCE_ID, request.routeGeoJson))
            .withLayer(
                LineLayer(SnapshotConfig.ROUTE_LAYER_ID, SnapshotConfig.ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineColor(baseColor),
                    PropertyFactory.lineWidth(ROUTE_WIDTH_DP),
                ),
            )

        // "이번에 처음 간 길"은 전체 경로를 깔고 그 위에 금색으로 덧그린다 — 경로를 새/옛 구간으로
        // 쪼개서 따로 그리면 이어지는 지점마다 선이 끊겨 보인다.
        request.highlightGeoJson?.let { highlight ->
            builder
                .withSource(GeoJsonSource(SnapshotConfig.HIGHLIGHT_SOURCE_ID, highlight))
                .withLayer(
                    LineLayer(SnapshotConfig.HIGHLIGHT_LAYER_ID, SnapshotConfig.HIGHLIGHT_SOURCE_ID).withProperties(
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineColor(newRoadColor),
                        PropertyFactory.lineWidth(ROUTE_WIDTH_DP),
                    ),
                )
        }

        val start = request.start
        val end = request.end
        if (request.variant == SnapshotVariant.RESULT_CARD && start != null && end != null) {
            val endpoints = FeatureCollection.fromFeatures(
                listOf(
                    Feature.fromGeometry(Point.fromLngLat(start.lon, start.lat)),
                    Feature.fromGeometry(Point.fromLngLat(end.lon, end.lat)),
                ),
            )
            builder
                .withSource(GeoJsonSource(SnapshotConfig.ENDPOINT_SOURCE_ID, endpoints))
                .withLayer(
                    CircleLayer(SnapshotConfig.ENDPOINT_LAYER_ID, SnapshotConfig.ENDPOINT_SOURCE_ID).withProperties(
                        PropertyFactory.circleRadius(ENDPOINT_RADIUS_DP),
                        PropertyFactory.circleColor(endpointColor),
                        PropertyFactory.circleStrokeWidth(ENDPOINT_STROKE_WIDTH_DP),
                        PropertyFactory.circleStrokeColor(endpointStrokeColor),
                    ),
                )
        }

        return builder
    }
}
