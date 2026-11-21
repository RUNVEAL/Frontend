package com.sloth.runveal.courserecommend

import android.util.Log
import com.sloth.runveal.auth.AndroidAppContextHolder
import com.sloth.runveal.core.map.CourseSnapshotRenderer
import com.sloth.runveal.courserecommend.contract.CourseDetailIntent
import com.sloth.runveal.courserecommend.contract.CourseDetailSource
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

/** 효과가 필요 없는 화면이라 Effect 타입 인자는 [Nothing]. */
class CourseDetailViewModel : MviViewModel<CourseDetailIntent, CourseDetailState, Nothing>(
    initialState = CourseDetailState.Loading,
) {
    private val database = RunvealDatabaseProvider.database
    private val json = Json
    private val snapshotRenderer = CourseSnapshotRenderer(context = AndroidAppContextHolder.context)

    /** [load]가 찾아 둔 원본 — [CourseDetailIntent.SaveCourse]가 DB에 쓸 때 다시 쓴다(화면 상태는
     * 포맷된 문자열뿐이라 원본 숫자/배열이 필요하다). */
    private var currentRoute: RouteDto? = null

    /** [resolveNewRoadMeters]가 정한 새 길 길이(m) — 저장할 때도 같은 값을 써야 "내 코스" 탭 카드가
     * 코스 상세와 같은 숫자를 보여준다. */
    private var currentNewRoadMeters: Double = 0.0

    override suspend fun handleIntent(intent: CourseDetailIntent) {
        when (intent) {
            is CourseDetailIntent.Load -> when (val source = intent.source) {
                is CourseDetailSource.Recommended -> load(source.id)
                is CourseDetailSource.Saved -> loadSavedCourse(source.id)
            }
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

    /**
     * "내 코스"에서 들어온 코스 — 저장할 때 추천 응답을 컬럼에 그대로 넣어 뒀으니 그걸 다시 읽어
     * 추천 결과와 같은 화면을 만든다. 러닝 기록을 승격한 코스는 추천 전용 컬럼(시간·고도·경유지)이
     * 전부 비어 있어서, 그 칸들은 빈 값으로 둔다([CourseDetailState.Ready] 주석의 "공란" 규칙).
     *
     * 이미 저장된 코스라 다시 저장할 일이 없으므로 `currentRoute`는 비워 둔다 — "저장됨" 버튼을
     * 눌러도 [saveCourse]가 그 자리에서 빠져나온다.
     */
    private suspend fun loadSavedCourse(courseId: String) {
        val course = withContext(Dispatchers.IO) {
            database.courseQueries.selectCourseById(courseId).executeAsOneOrNull()
        }
        if (course == null) {
            Log.w(TAG, "저장된 코스를 못 찾았다 — courseId=$courseId")
            setState { CourseDetailState.NotFound }
            return
        }
        val durationMinutes = course.duration_sec?.let { it / 60 }
        val elevationGain = course.elevation_gain_m
        setState {
            CourseDetailState.Ready(
                courseLabel = "",
                savedCourseName = course.name,
                distanceValue = "%.1f".format(course.distance_meters / 1000.0),
                distanceUnit = "km",
                timeValue = durationMinutes?.toString() ?: "",
                timeUnit = if (durationMinutes != null) "분" else "",
                elevationValue = elevationGain?.let { "+$it" } ?: "",
                elevationUnit = if (elevationGain != null) "m" else "",
                newRoadValue = formatNewRoadPercent(
                    course.new_road_ratio ?: (course.new_road_m ?: 0L).toDouble().safeRatio(course.distance_meters),
                ),
                newRoadUnit = "%",
                elevationProfile = course.elevation_profile?.let { decodeElevationProfile(it) } ?: emptyList(),
                routeSteps = buildRouteSteps(
                    milestones = course.milestones?.let { decodeMilestones(it) } ?: emptyList(),
                    distanceMeters = course.distance_meters,
                    isLoop = course.route_type == "LOOP",
                ),
                path = decodeCoordinates(course.coordinates),
                isLoop = course.route_type == "LOOP",
                isSaved = true,
            )
        }
    }

    /** `course.coordinates`("[[lon,lat],…]", 저장할 때 인코딩한 형식)를 되돌린다 — GeoJSON 좌표
     * 순서라 [lng, lat]여서 LatLon은 반대로 매핑한다("내 코스" 탭과 같은 처리). */
    private fun decodeCoordinates(raw: String): List<LatLon> =
        json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
            .map { LatLon(lat = it[1], lon = it[0]) }

    /** 0~1 비율을 "24"처럼 정수 퍼센트 문자열로. 소수점을 붙이면 네 칸짜리 통계 줄에서 넘친다. */
    private fun formatNewRoadPercent(ratio: Double): String = "%.0f".format((ratio * 100).coerceIn(0.0, 100.0))

    /** 거리가 0이면(저장만 하고 좌표가 비정상인 코스 등) 0으로 둔다 — 나눗셈이 NaN이 되면 "NaN%"가 찍힌다. */
    private fun Double.safeRatio(total: Double): Double = if (total > 0) this / total else 0.0

    private fun decodeElevationProfile(raw: String): List<ElevationPointUiModel> =
        json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
            .mapNotNull { point ->
                if (point.size >= 2) ElevationPointUiModel(cumulativeDistanceM = point[0], elevationM = point[1]) else null
            }

    private fun decodeMilestones(raw: String): List<MilestoneDto> =
        json.decodeFromString(ListSerializer(MilestoneDto.serializer()), raw)

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
        prerenderThumbnail(courseId, ready.path, isLoop = route.type == "LOOP")
        setState { if (this is CourseDetailState.Ready) copy(isSaved = true) else this }
    }

    /** "내 코스" 탭 목록·추천 카드가 쓸 썸네일을 저장 시점에 같이 만들어 캐시해 둔다. 실패해도 코스
     * 저장 자체는 이미 끝났으니 조용히 무시한다 — 목록은 캐시가 없으면 플레이스홀더를 보여준다. */
    private suspend fun prerenderThumbnail(courseId: String, path: List<LatLon>, isLoop: Boolean) {
        // 목록("내 코스" 탭)이 만드는 요청과 **같아야** 미리 만든 캐시를 그대로 쓴다 — 캐시 키에
        // 점 정보가 안 들어가서, 한쪽만 도착 점을 켜면 먼저 그린 그림이 그대로 보인다.
        val request = path.toSnapshotRequest(
            ownerId = courseId,
            variant = SnapshotVariant.THUMBNAIL,
            withStartMarker = true,
            withEndMarker = !isLoop,
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
            // 시안(B2)의 "새 길"은 거리가 아니라 **코스에서 처음 가는 길의 비율**이다.
            newRoadValue = formatNewRoadPercent(newRoadRatio ?: newRoadMeters.safeRatio(distanceM.toDouble())),
            newRoadUnit = "%",
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
    private fun RouteDto.buildRouteSteps(): List<RouteStepUiModel> =
        buildRouteSteps(milestones = milestones, distanceMeters = distanceM.toDouble(), isLoop = type == "LOOP")

    /** 출발 — 경유지들 — 도착. 추천 응답과 저장된 코스가 같은 모양으로 보이게 한 곳에서 만든다. */
    private fun buildRouteSteps(
        milestones: List<MilestoneDto>,
        distanceMeters: Double,
        isLoop: Boolean,
    ): List<RouteStepUiModel> {
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
            note = if (isLoop) "출발지" else null,
            distanceLabel = "%.1fkm".format(distanceMeters / 1000.0),
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
