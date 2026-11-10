package com.swyp.runveal.valhalla

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `feature-valhalla-matching.md` "요청/응답" — Valhalla `trace_attributes` 요청/응답 DTO. */
@Serializable
data class TraceRequest(
    val shape: List<TracePointDto>,
    val costing: String = "pedestrian",
    @SerialName("shape_match") val shapeMatch: String = "map_snap",
    val filters: TraceFiltersDto = TraceFiltersDto(),
)

@Serializable
data class TracePointDto(val lat: Double, val lon: Double)

@Serializable
data class TraceFiltersDto(
    // "edge.begin_shape_index"/"edge.end_shape_index"가 없으면 Valhalla가 그 둘을 응답에서 아예
    // 빼버려서(action=include라 요청한 attribute만 옴) TraceResponseMapper.sliceEdge가 매번 null을
    // 돌려받아 edge를 전부 버린다 — 실기기 trace_attributes 호출로 재현·확인한 버그.
    val attributes: List<String> = listOf(
        "edge.way_id",
        "edge.length",
        "edge.begin_shape_index",
        "edge.end_shape_index",
        "shape",
        "matched.point",
        "matched.type",
        "matched.edge_index",
        "matched.distance_along_edge",
    ),
    val action: String = "include",
)

@Serializable
data class TraceResponse(
    val shape: String? = null,
    val edges: List<EdgeDto> = emptyList(),
    @SerialName("matched_points") val matchedPoints: List<MatchedPointDto> = emptyList(),
    @SerialName("confidence_score") val confidenceScore: Double = 0.0,
)

@Serializable
data class EdgeDto(
    @SerialName("way_id") val wayId: Long? = null,
    @SerialName("begin_shape_index") val beginShapeIndex: Int? = null,
    @SerialName("end_shape_index") val endShapeIndex: Int? = null,
)

@Serializable
data class MatchedPointDto(
    val lat: Double? = null,
    val lon: Double? = null,
    val type: String? = null,
)
