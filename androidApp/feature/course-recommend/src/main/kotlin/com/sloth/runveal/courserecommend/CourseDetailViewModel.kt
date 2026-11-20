package com.sloth.runveal.courserecommend

import android.util.Log
import com.sloth.runveal.auth.AndroidAppContextHolder
import com.sloth.runveal.core.map.CourseSnapshotRenderer
import com.sloth.runveal.courserecommend.contract.CourseDetailIntent
import com.sloth.runveal.courserecommend.contract.CourseDetailState
import com.sloth.runveal.courserecommend.contract.ElevationPointUiModel
import com.sloth.runveal.courserecommend.contract.RouteStepUiModel
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.designsystem.component.list.RunvealRouteStepType
import com.sloth.runveal.dong.NewRoadEstimator
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.toSnapshotRequest
import com.sloth.runveal.network.routes.MilestoneDto
import com.sloth.runveal.network.routes.RouteDto
import com.sloth.runveal.ui.mvi.MviViewModel
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val TAG = "CourseDetailDebug"

/** 코스 목록(저장된 코스) 썸네일 자리 크기 — [CourseSnapshotRenderer]의 코스 목록 72dp 컨벤션과 맞춘다. */
private const val THUMBNAIL_SIZE_DP = 72

/** Compose 컨텍스트가 없는 ViewModel이라 `RunvealTheme.colors`를 못 읽어서, [CourseSnapshotImage]와
 * 같은 디자인 토큰을 ARGB로 직접 옮겨 둔다 — map.roadExplored(gold200)/map.location(blue500)/
 * text.primary(neutral50). */
private const val SNAPSHOT_ROUTE_COLOR = 0xFFF2E096.toInt()
private const val SNAPSHOT_ENDPOINT_COLOR = 0xFF4286E0.toInt()
private const val SNAPSHOT_ENDPOINT_STROKE_COLOR = 0xFFF4EEEA.toInt()

/** 출발지 점 — accent/glow(gold200) 점에 bg/map(neutral900) 테두리. [CourseSnapshotImage]와 같은 값이라야
 * 같은 캐시 파일을 함께 쓴다. */
private const val SNAPSHOT_START_COLOR = 0xFFF2E096.toInt()
private const val SNAPSHOT_START_STROKE_COLOR = 0xFF0E0E0E.toInt()

/** 효과가 필요 없는 화면이라 Effect 타입 인자는 [Nothing]. */
class CourseDetailViewModel : MviViewModel<CourseDetailIntent, CourseDetailState, Nothing>(
    initialState = CourseDetailState.Loading,
) {
    private val database = RunvealDatabaseProvider.database
    private val json = Json
    private val snapshotRenderer = CourseSnapshotRenderer(
        context = AndroidAppContextHolder.context,
        routeColor = SNAPSHOT_ROUTE_COLOR,
        startColor = SNAPSHOT_START_COLOR,
        startStrokeColor = SNAPSHOT_START_STROKE_COLOR,
        endpointColor = SNAPSHOT_ENDPOINT_COLOR,
        endpointStrokeColor = SNAPSHOT_ENDPOINT_STROKE_COLOR,
        // 코스 썸네일은 러닝이 아니라서(isRunRoute=false) 아래 두 색은 실제로 안 쓰인다 —
        // 러닝 경로에서만 쓰는 값이라 경로 색을 그대로 넘긴다.
        newRoadColor = SNAPSHOT_ROUTE_COLOR,
        todayRoadColor = SNAPSHOT_ROUTE_COLOR,
    )

    /** [load]가 찾아 둔 원본 — [CourseDetailIntent.SaveCourse]가 DB에 쓸 때 다시 쓴다(화면 상태는
     * 포맷된 문자열뿐이라 원본 숫자/배열이 필요하다). */
    private var currentRoute: RouteDto? = null

    /** [resolveNewRoadMeters]가 정한 새 길 길이(m) — 저장할 때도 같은 값을 써야 "내 코스" 탭 카드가
     * 코스 상세와 같은 숫자를 보여준다. */
    private var currentNewRoadMeters: Double = 0.0

    override suspend fun handleIntent(intent: CourseDetailIntent) {
        when (intent) {
            is CourseDetailIntent.Load -> load(intent.routeId)
            CourseDetailIntent.SaveCourse -> saveCourse()
        }
    }

    private suspend fun load(routeId: String) {
        val response = (CourseRecommendResultHolder.state.value as? CourseRecommendResult.Success)?.response
        val index = response?.routes?.indexOfFirst { it.routeId == routeId } ?: -1
        val route = response?.routes?.getOrNull(index)
        currentRoute = route
        if (route == null) {
            setState { CourseDetailState.NotFound }
            return
        }
        currentNewRoadMeters = resolveNewRoadMeters(route)
        setState { route.toDetailState(courseLabel = ('A' + index).toString(), newRoadMeters = currentNewRoadMeters) }
    }

    /** 서버가 `newRoadM`을 줬으면 그대로 쓰고, 안 줬으면(요청에 `exploredSegmentIds`를 못 보냈거나
     * 서버가 아직 안 채워 줄 때 — `docs/api/API.md` §3.3.1) 로컬 `explored_segment`로 추정한다. */
    private suspend fun resolveNewRoadMeters(route: RouteDto): Double =
        route.newRoadM?.toDouble() ?: withContext(Dispatchers.IO) {
            NewRoadEstimator.newRoadMeters(
                totalDistanceM = route.distanceM.toDouble(),
                exploredLengthM = exploredLengthFor(route.segmentIds),
            )
        }

    /** 빈 목록이면 쿼리가 `IN ()`이 돼서 SQLite 문법 오류가 나므로 먼저 거른다. */
    private fun exploredLengthFor(segmentIds: List<Long>): Double =
        if (segmentIds.isEmpty()) 0.0
        else database.exploredSegmentQueries.exploredLengthForSegments(segmentIds).executeAsOne()

    /** `docs/local-db/local-db-schema-design.md` §3.4 `course` 테이블에 추천 응답을 그대로 저장한다.
     * 러닝 기록을 승격한 코스와 달리 `source_track_id`가 없어서, 추천 전용 컬럼(거리·시간·고도·태그·
     * 경유지 등)을 채운다. 이미 저장했으면(중복 탭) 다시 넣지 않는다. 썸네일 렌더링도 같은 호출 안에서
     * 끝까지 기다린다 — 따로 코루틴을 띄우면 저장 직후 화면을 벗어날 때 ViewModel scope가 취소되면서
     * 캐시가 안 만들어질 수 있다. */
    private suspend fun saveCourse() {
        val route = currentRoute ?: return
        val ready = currentState as? CourseDetailState.Ready ?: return
        if (ready.isSaved) return
        val start = route.path.firstOrNull() ?: return
        val courseId = UUID.randomUUID().toString()

        database.courseQueries.insertCourse(
            id = courseId,
            source_track_id = null,
            name = "코스 ${ready.courseLabel}",
            distance_meters = route.distanceM.toDouble(),
            // [lng, lat] 순서가 이미 coordinates 컬럼이 기대하는 "[[lon,lat],…]"와 같다.
            coordinates = encodeDoubleMatrix(route.path),
            start_lat = start[1],
            start_lon = start[0],
            place_name = null,
            created_at_epoch_ms = System.currentTimeMillis(),
            route_type = route.type,
            duration_sec = route.durationSec.toLong(),
            elevation_gain_m = route.elevationGainM?.toLong(),
            elevation_profile = route.elevationProfile?.let { encodeDoubleMatrix(it) },
            // 서버가 안 준 경우엔 [resolveNewRoadMeters]의 로컬 추정값을 그대로 저장한다 — "내 코스"
            // 탭이 이 컬럼만 읽어서 새 길을 보여주기 때문에 여기서 비워 두면 목록에서만 값이 사라진다.
            new_road_m = currentNewRoadMeters.roundToLong(),
            new_road_ratio = route.newRoadRatio
                ?: (currentNewRoadMeters / route.distanceM).takeIf { route.distanceM > 0 },
            tags = json.encodeToString(ListSerializer(String.serializer()), route.tags),
            segment_ids = json.encodeToString(ListSerializer(Long.serializer()), route.segmentIds),
            milestones = json.encodeToString(ListSerializer(MilestoneDto.serializer()), route.milestones),
        )
        prerenderThumbnail(courseId, ready.path)
        setState { if (this is CourseDetailState.Ready) copy(isSaved = true) else this }
    }

    /** "내 코스" 탭 목록·추천 카드가 쓸 썸네일을 저장 시점에 같이 만들어 캐시해 둔다. 실패해도 코스
     * 저장 자체는 이미 끝났으니 조용히 무시한다 — 목록은 캐시가 없으면 플레이스홀더를 보여준다. */
    private suspend fun prerenderThumbnail(courseId: String, path: List<LatLon>) {
        val request = path.toSnapshotRequest(
            ownerId = courseId,
            variant = SnapshotVariant.THUMBNAIL,
            withStartMarker = true,
        ) ?: return
        val density = AndroidAppContextHolder.context.resources.displayMetrics.density
        val sizePx = (THUMBNAIL_SIZE_DP * density).roundToInt()
        val file = snapshotRenderer.getOrRender(request, widthPx = sizePx, heightPx = sizePx)
        if (file == null) Log.w(TAG, "코스 썸네일 렌더링 실패: courseId=$courseId")
    }

    private fun encodeDoubleMatrix(values: List<List<Double>>): String =
        json.encodeToString(ListSerializer(ListSerializer(Double.serializer())), values)

    /** [newRoadMeters]는 서버 `newRoadM` 또는 [resolveNewRoadMeters]의 로컬 추정값(m). */
    private fun RouteDto.toDetailState(courseLabel: String, newRoadMeters: Double): CourseDetailState.Ready {
        val elevation = elevationGainM
        return CourseDetailState.Ready(
            courseLabel = courseLabel,
            distanceValue = "%.1f".format(distanceM / 1000.0),
            distanceUnit = "km",
            timeValue = (durationSec / 60).toString(),
            timeUnit = "분",
            // 값이 없으면 "-" 같은 대체 텍스트 없이 그냥 공란 — RunvealStat이 weight로 칸 폭을 맞춘다.
            elevationValue = elevation?.let { "+$it" } ?: "",
            elevationUnit = if (elevation != null) "m" else "",
            newRoadValue = "%.1f".format(newRoadMeters / 1000.0),
            newRoadUnit = "km",
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

    /** `NEW_ROAD`는 제목을 "새 길"로 두고, 서버가 주는 도로명([MilestoneDto.name], §3.3.2)을 그 옆에
     * 덧붙인다 — 이름을 못 받았으면 예전처럼 "새로 탐험하는 길"로 메운다. 나머지 `kind`는 제목이
     * 장소명이고 옆 문구는 앱이 정한다. 강조 스타일은 이름과 무관하게 `kind` 기준. */
    private fun MilestoneDto.toRouteStep(): RouteStepUiModel {
        val isNewRoad = kind == "NEW_ROAD"
        val placeName = name?.takeIf { it.isNotBlank() }
        val place = if (isNewRoad) {
            "새 길"
        } else {
            placeName ?: when (kind) {
                "PARK" -> "공원"
                "RIVER" -> "하천길"
                else -> "도로"
            }
        }
        val note = when (kind) {
            "PARK" -> "공원 지나가기"
            "RIVER" -> "하천길 따라가기"
            "NEW_ROAD" -> placeName ?: "새로 탐험하는 길"
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
