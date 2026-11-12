package com.swyp.runveal.courserecommend

import com.swyp.runveal.courserecommend.contract.CourseDetailIntent
import com.swyp.runveal.courserecommend.contract.CourseDetailState
import com.swyp.runveal.courserecommend.contract.ElevationPointUiModel
import com.swyp.runveal.courserecommend.contract.RouteStepUiModel
import com.swyp.runveal.designsystem.component.list.RunvealRouteStepType
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.network.routes.MilestoneDto
import com.swyp.runveal.network.routes.RouteDto
import com.swyp.runveal.ui.mvi.MviViewModel

/** 효과가 필요 없는 화면이라 Effect 타입 인자는 [Nothing]. */
class CourseDetailViewModel : MviViewModel<CourseDetailIntent, CourseDetailState, Nothing>(
    initialState = CourseDetailState.Loading,
) {
    override suspend fun handleIntent(intent: CourseDetailIntent) {
        when (intent) {
            is CourseDetailIntent.Load -> load(intent.routeId)
        }
    }

    private fun load(routeId: String) {
        val response = (CourseRecommendResultHolder.state.value as? CourseRecommendResult.Success)?.response
        val index = response?.routes?.indexOfFirst { it.routeId == routeId } ?: -1
        val route = response?.routes?.getOrNull(index)
        setState {
            if (route != null) route.toDetailState(courseLabel = ('A' + index).toString()) else CourseDetailState.NotFound
        }
    }

    private fun RouteDto.toDetailState(courseLabel: String): CourseDetailState.Ready {
        val elevation = elevationGainM
        val newRoad = newRoadM
        return CourseDetailState.Ready(
            courseLabel = courseLabel,
            distanceValue = "%.1f".format(distanceM / 1000.0),
            distanceUnit = "km",
            timeValue = (durationSec / 60).toString(),
            timeUnit = "분",
            // 값이 없으면 "-" 같은 대체 텍스트 없이 그냥 공란 — RunvealStat이 weight로 칸 폭을 맞춘다.
            elevationValue = elevation?.let { "+$it" } ?: "",
            elevationUnit = if (elevation != null) "m" else "",
            newRoadValue = newRoad?.let { "%.1f".format(it / 1000.0) } ?: "",
            newRoadUnit = if (newRoad != null) "km" else "",
            elevationProfile = elevationProfile?.mapNotNull { point ->
                if (point.size >= 2) ElevationPointUiModel(cumulativeDistanceM = point[0], elevationM = point[1]) else null
            } ?: emptyList(),
            routeSteps = buildRouteSteps(),
            // GeoJSON 좌표 순서라 [lng, lat](§5.2) — LatLon은 반대 순서.
            path = path.map { LatLon(lat = it[1], lon = it[0]) },
            isLoop = type == "LOOP",
        )
    }

    /** "출발"·중간 경유지(milestones)·"도착" 순서로 경로 정보 목록을 만든다. */
    private fun RouteDto.buildRouteSteps(): List<RouteStepUiModel> {
        val steps = mutableListOf<RouteStepUiModel>()
        steps += RouteStepUiModel(
            place = "출발",
            note = "현재 위치",
            distanceLabel = "0.0km",
            type = RunvealRouteStepType.Start,
        )
        steps += milestones.map { it.toRouteStep() }
        steps += RouteStepUiModel(
            place = "도착",
            note = if (type == "LOOP") "출발지" else null,
            distanceLabel = "%.1fkm".format(distanceM / 1000.0),
            type = RunvealRouteStepType.End,
        )
        return steps
    }

    /** `kind`별로 이름이 없을 때(특히 `NEW_ROAD`는 항상 없음, §3.6.2) 보여줄 문구를 앱이 정한다. */
    private fun MilestoneDto.toRouteStep(): RouteStepUiModel {
        val isNewRoad = kind == "NEW_ROAD"
        val place = name ?: when (kind) {
            "PARK" -> "공원"
            "RIVER" -> "하천길"
            "NEW_ROAD" -> "새 길"
            else -> "도로"
        }
        val note = when (kind) {
            "PARK" -> "공원 지나가기"
            "RIVER" -> "하천길 따라가기"
            "NEW_ROAD" -> "새로 탐험하는 길"
            else -> null
        }
        return RouteStepUiModel(
            place = place,
            note = note,
            distanceLabel = "%.1fkm".format(startM / 1000.0),
            type = if (isNewRoad) RunvealRouteStepType.NewRoad else RunvealRouteStepType.Waypoint,
        )
    }
}
