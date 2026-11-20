package com.sloth.runveal.core.map.internal.snapshot

import com.sloth.runveal.core.map.SnapshotColors
import com.sloth.runveal.map.LatLon
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

/** 앱 지도와 같은 스타일([MapConfig.STYLE_URL]) 위에 경로 선 + 끝점 레이어를 얹는다
 * (RESULT_CARD는 출발·도착 두 점, 썸네일은 출발지 한 점). [VisitedRoadLayer]와 같은 패턴(`PropertyFactory` + 둥근 cap/join)이지만, 스냅샷은 줌 보간이
 * 필요 없는 고정 프레임 1장이라 두께를 상수로 바로 쓴다. */
internal object SnapshotStyleBuilder {
    /** 썸네일은 작게 줄여 보여주는 그림이라 지도 위 선(3dp)보다 굵게 그려야 경로가 읽힌다. */
    private const val ROUTE_WIDTH_DP = 4f
    private const val ENDPOINT_RADIUS_DP = 5f
    private const val ENDPOINT_STROKE_WIDTH_DP = 2f

    /** 출발지 점 테두리 두께 = 반지름 x 이 비율. 디자인시스템 출발 마커(반지름 7 : 테두리 2)의 비율이라,
     * 점을 키워도 테두리가 실처럼 얇아지지 않는다. */
    private const val START_STROKE_RATIO = 2f / 7f

    fun build(
        request: SnapshotRequest,
        colors: SnapshotColors,
        endpointRadiusDp: Float,
    ): Style.Builder {
        // 러닝 기록은 "오늘 달린 길"(흰색) 위에 "새로 탐험한 길"(금색)을 덧그려 둘을 구분한다 —
        // 결과 화면 범례가 그 두 색으로 설명하고 있어서 기본선도 범례와 같아야 한다. 아직 달리지
        // 않은 코스는 그 구분이 없어 경로색(금색) 한 가지로 그린다.
        val baseColor = if (request.isRunRoute) colors.todayRoad else colors.route
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
                        PropertyFactory.lineColor(colors.newRoad),
                        PropertyFactory.lineWidth(ROUTE_WIDTH_DP),
                    ),
                )
        }

        val start = request.start
        val end = request.end
        // 코스 썸네일은 디자인시스템 마커와 같은 색으로 — 출발은 금색, 도착은 흰색(둘 다 어두운 테두리).
        // 카드가 그림 **한가운데**에 점을 얹던 걸 대신한다(그땐 실제 출발지와 무관했다). 도착점은
        // 출발과 같은 자리면(순환 코스) 아예 안 실려 오므로 여기선 있는 것만 그린다.
        if (request.variant != SnapshotVariant.RESULT_CARD) {
            // 도착을 먼저 깔아 두 점이 가까울 때 출발이 위에 오게 한다 — 출발이 더 중요한 정보다.
            end?.let { point ->
                builder.addEndpointCircle(
                    sourceId = SnapshotConfig.END_SOURCE_ID,
                    layerId = SnapshotConfig.END_LAYER_ID,
                    point = point,
                    radiusDp = endpointRadiusDp,
                    color = colors.end,
                    strokeColor = colors.endStroke,
                )
            }
            start?.let { point ->
                builder.addEndpointCircle(
                    sourceId = SnapshotConfig.START_SOURCE_ID,
                    layerId = SnapshotConfig.START_LAYER_ID,
                    point = point,
                    radiusDp = endpointRadiusDp,
                    color = colors.start,
                    strokeColor = colors.startStroke,
                )
            }
        }
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
                        PropertyFactory.circleColor(colors.resultEndpoint),
                        PropertyFactory.circleStrokeWidth(ENDPOINT_STROKE_WIDTH_DP),
                        PropertyFactory.circleStrokeColor(colors.resultEndpointStroke),
                    ),
                )
        }

        return builder
    }

    /** 코스 썸네일의 출발·도착 점 한 개. 테두리는 반지름에 비례해 점을 키워도 실처럼 얇아지지 않는다. */
    private fun Style.Builder.addEndpointCircle(
        sourceId: String,
        layerId: String,
        point: LatLon,
        radiusDp: Float,
        color: Int,
        strokeColor: Int,
    ): Style.Builder = withSource(
        GeoJsonSource(
            sourceId,
            FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(point.lon, point.lat)))),
        ),
    ).withLayer(
        CircleLayer(layerId, sourceId).withProperties(
            PropertyFactory.circleRadius(radiusDp),
            PropertyFactory.circleColor(color),
            PropertyFactory.circleStrokeWidth(radiusDp * START_STROKE_RATIO),
            PropertyFactory.circleStrokeColor(strokeColor),
        ),
    )
}
