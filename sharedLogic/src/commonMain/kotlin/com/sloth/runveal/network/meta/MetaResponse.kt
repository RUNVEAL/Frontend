package com.sloth.runveal.network.meta

import kotlinx.serialization.Serializable

/** `GET /api/v1/meta` 응답 — `docs/api/API.md` §3.1.1. */
@Serializable
data class MetaResponse(
    val build: String,
    val mapVersions: MapVersions,
    val limits: MetaLimits,
    val routes: RoutesMeta,
)

@Serializable
data class MapVersions(
    val current: String,
    val supported: List<String>,
)

@Serializable
data class MetaLimits(
    val maxBodyBytes: Long,
    val match: MatchLimits,
    val routes: RoutesLimits,
)

@Serializable
data class MatchLimits(
    val minPoints: Int,
    val maxPoints: Int,
)

@Serializable
data class RoutesLimits(
    val distanceM: RangeLimit,
    val paceSecPerKm: RangeLimit,
    val maxExploredSegmentIds: Int,
    val maxRoutes: Int,
)

/** `step`이 `null`이면 자유 입력(`paceSecPerKm`가 그 경우). */
@Serializable
data class RangeLimit(
    val min: Int,
    val max: Int,
    val step: Int? = null,
    val default: Int,
)

@Serializable
data class RoutesMeta(
    val filters: List<String>,
    val tags: List<String>,
    val loopToleranceRatio: Double,
    val oneWayToleranceRatio: Double,
)
