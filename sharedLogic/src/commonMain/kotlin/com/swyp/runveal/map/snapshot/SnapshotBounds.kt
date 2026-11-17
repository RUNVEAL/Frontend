package com.swyp.runveal.map.snapshot

import com.swyp.runveal.geojson.toMultiLineGeoJson
import com.swyp.runveal.geojson.toRouteGeoJson
import com.swyp.runveal.map.LatLon
import kotlin.math.PI
import kotlin.math.cos

private const val METERS_PER_DEGREE_LAT = 111_320.0

private fun metersPerDegreeLon(latDegrees: Double): Double = METERS_PER_DEGREE_LAT * cos(latDegrees / 180.0 * PI)

/** 경로 bbox → 여백(§2 `PADDING_RATIO`) → 최소 범위(§2 `MIN_SPAN_METERS`) 순서로 적용한다
 * (`docs/running/COURSE_MAP_SNAPSHOT.md` §2). 최소 범위는 위도에 따른 경도 보정을 거쳐 적용된다 —
 * 그래서 짧은 코스나 제자리 러닝도 일정한 "실제 거리" 만큼은 화면에 보인다. */
fun List<LatLon>.snapshotBounds(): GeoBounds {
    require(isNotEmpty()) { "snapshotBounds() requires at least one point" }

    var south = this[0].lat
    var north = this[0].lat
    var west = this[0].lon
    var east = this[0].lon
    for (point in this) {
        if (point.lat < south) south = point.lat
        if (point.lat > north) north = point.lat
        if (point.lon < west) west = point.lon
        if (point.lon > east) east = point.lon
    }

    val latPadding = (north - south) * SnapshotConfig.PADDING_RATIO
    val lonPadding = (east - west) * SnapshotConfig.PADDING_RATIO
    south -= latPadding
    north += latPadding
    west -= lonPadding
    east += lonPadding

    val centerLat = (south + north) / 2
    val minSpanDegreesLat = SnapshotConfig.MIN_SPAN_METERS / METERS_PER_DEGREE_LAT
    val minSpanDegreesLon = SnapshotConfig.MIN_SPAN_METERS / metersPerDegreeLon(centerLat)

    if (north - south < minSpanDegreesLat) {
        val extra = (minSpanDegreesLat - (north - south)) / 2
        south -= extra
        north += extra
    }
    if (east - west < minSpanDegreesLon) {
        val extra = (minSpanDegreesLon - (east - west)) / 2
        west -= extra
        east += extra
    }

    return GeoBounds(south = south, west = west, north = north, east = east)
}

/**
 * 점이 1개 이하인 경로는 스냅샷 요청을 만들지 않는다.
 *
 * [newRoadPaths]로 **러닝 기록인지 코스인지**가 갈린다:
 * - `null` — 아직 달리지 않은 코스. 경로를 한 색으로만 그린다.
 * - 목록(비어 있어도 됨) — 러닝 기록. 경로를 "오늘 달린 길"(흰색)로 깔고, 그중 처음 간 구간을
 *   "새로 탐험한 길"(금색)로 덧그린다. 같은 길을 또 달렸으면 목록이 비고 흰색만 남는다.
 */
fun List<LatLon>.toSnapshotRequest(
    ownerId: String,
    variant: SnapshotVariant,
    newRoadPaths: List<List<LatLon>>? = null,
): SnapshotRequest? {
    if (size <= 1) return null
    val highlight = newRoadPaths?.filter { it.size >= 2 }
    return SnapshotRequest(
        ownerId = ownerId,
        variant = variant,
        routeGeoJson = toRouteGeoJson(),
        bounds = snapshotBounds(),
        start = if (variant == SnapshotVariant.RESULT_CARD) first() else null,
        end = if (variant == SnapshotVariant.RESULT_CARD) last() else null,
        highlightGeoJson = highlight?.takeIf { it.isNotEmpty() }?.toMultiLineGeoJson(),
        isRunRoute = newRoadPaths != null,
    )
}

/**
 * 경로가 여러 줄인 스냅샷 — Health Connect 기록 불러오기 결과처럼 **여러 기록을 한 장에** 담을 때
 * 쓴다. 시작·끝 마커는 두지 않는다(여러 기록 중 어느 것의 시작인지 가릴 수 없다).
 *
 * 점이 2개 미만인 줄은 선이 되지 않아 빼고, 남은 게 없으면 요청을 만들지 않는다
 * ([toSnapshotRequest]가 점 1개 이하를 거르는 것과 같은 이유).
 */
fun List<List<LatLon>>.toMultiRouteSnapshotRequest(
    ownerId: String,
    variant: SnapshotVariant,
    newRoadPaths: List<List<LatLon>>,
): SnapshotRequest? {
    val routes = filter { it.size >= 2 }
    if (routes.isEmpty()) return null
    return SnapshotRequest(
        ownerId = ownerId,
        variant = variant,
        routeGeoJson = routes.toMultiLineGeoJson(),
        bounds = routes.flatten().snapshotBounds(),
        start = null,
        end = null,
        highlightGeoJson = newRoadPaths.filter { it.size >= 2 }.takeIf { it.isNotEmpty() }?.toMultiLineGeoJson(),
        isRunRoute = true,
    )
}
