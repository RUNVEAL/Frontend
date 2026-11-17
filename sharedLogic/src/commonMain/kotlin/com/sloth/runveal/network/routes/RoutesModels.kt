package com.sloth.runveal.network.routes

import kotlinx.serialization.Serializable

/** `POST /api/v1/routes` 요청 — `docs/api/API.md` §3.3.1. `start`/`end`는 `{lat, lng}` 객체(§5.2 좌표
 * 순서 주의 — `/match`의 `points`·응답 `path`와 반대). */
@Serializable
data class RoutesRequest(
    val mapVersion: String,
    val start: LatLng,
    val end: LatLng? = null,
    val distanceM: Int? = null,
    val preferUnexplored: Boolean? = null,
    val exploredSegmentIds: List<Long>? = null,
    val filters: List<String>? = null,
    val paceSecPerKm: Int? = null,
)

@Serializable
data class LatLng(
    val lat: Double,
    val lng: Double,
)

/** `POST /api/v1/routes` 응답. `routes`가 빈 배열이면 에러가 아니라 §3.3.3 케이스(`warnings` 참고). */
@Serializable
data class RoutesResponse(
    val mapVersion: String,
    val routes: List<RouteDto>,
    val warnings: List<String>,
    val shortestDistanceM: Long? = null,
)

@Serializable
data class RouteDto(
    val routeId: String,
    val type: String,
    val distanceM: Int,
    val durationSec: Int,
    val elevationGainM: Int? = null,
    /** `[누적거리m, 고도m]` 100m 간격. */
    val elevationProfile: List<List<Double>>? = null,
    val newRoadM: Int? = null,
    val newRoadRatio: Double? = null,
    val tags: List<String>,
    /** `[lng, lat]` — GeoJSON 좌표 순서(§5.2). */
    val path: List<List<Double>>,
    val segmentIds: List<Long>,
    val milestones: List<MilestoneDto>,
)

@Serializable
data class MilestoneDto(
    val name: String? = null,
    val kind: String,
    val startM: Int,
    val endM: Int,
)
