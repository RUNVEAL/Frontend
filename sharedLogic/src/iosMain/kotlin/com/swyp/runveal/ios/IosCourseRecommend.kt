package com.swyp.runveal.ios

import com.swyp.runveal.data.local.RunvealDatabaseProvider
import com.swyp.runveal.dong.SegmentCoverageMatcher
import com.swyp.runveal.dong.NewRoadEstimator
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.snapshot.SnapshotRequest
import com.swyp.runveal.map.snapshot.SnapshotVariant
import com.swyp.runveal.network.meta.MetaApi
import com.swyp.runveal.network.meta.MetaHolder
import com.swyp.runveal.network.routes.LatLng
import com.swyp.runveal.network.routes.MilestoneDto
import com.swyp.runveal.network.routes.RouteDto
import com.swyp.runveal.network.routes.RoutesApi
import com.swyp.runveal.network.routes.RoutesRequest
import com.swyp.runveal.network.routes.RoutesResponse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import kotlin.math.roundToLong

/**
 * 코스 추천 — `/api/v1/routes` 호출과 로컬 저장을 iOS에서 쓰기 좋게 감싼 것.
 * Android `CourseCreateViewModel`/`CourseDetailViewModel`의 네트워크·DB 부분에 해당한다.
 */
object IosCourseRecommend {

    private val routesApi = RoutesApi()
    private val database get() = RunvealDatabaseProvider.database
    private val json = Json

    /**
     * `docs/api/API.md` §3.3 — B0 설정값으로 `/api/v1/routes`를 호출한다.
     *
     * [end]는 지도에서 도착지를 직접 고른 경우에만 넘긴다 — 안 고르면 null이라 자동으로 순환(LOOP)
     * 코스가 된다(§3.3.1). 화면 라벨(한글)인 필터는 서버 필터 코드와 1:1로 안 맞아서 아직 안 보낸다.
     */
    suspend fun fetchRoutes(
        start: LatLon,
        end: LatLon?,
        distanceMeters: Int,
        preferUnexplored: Boolean,
    ): RoutesResponse {
        val meta = MetaHolder.state.value ?: MetaApi().fetchMeta().also { MetaHolder.publish(it) }
        return routesApi.fetchRoutes(
            RoutesRequest(
                mapVersion = meta.mapVersions.current,
                start = LatLng(lat = start.lat, lng = start.lon),
                end = end?.let { LatLng(lat = it.lat, lng = it.lon) },
                distanceM = distanceMeters,
                preferUnexplored = preferUnexplored,
                exploredSegmentIds = exploredSegmentIds(meta.limits.routes.maxExploredSegmentIds),
            ),
        )
    }

    /**
     * 지금까지 달려서 "가본 길"이 된 구간 ID — 서버가 코스별 `newRoadM`을 계산하려면 이걸 같이 보내야
     * 한다(§3.3.1 — 안 보내면 null로 온다). 가본 게 하나도 없으면 빈 배열을 보낸다.
     *
     * **일부만 밟은 구간은 보내지 않는다**(`selectCoveredSegmentIds`) — 서버는 구간 단위로만 새 길을
     * 계산해서, 10%만 달린 구간을 보내면 남은 90%가 새 길에서 빠진다. 기준은 manifest
     * `segmentCoverage`와 같은 비율이다.
     *
     * [maxCount]를 넘기면 413 `TOO_MANY_SEGMENTS`라 앞에서부터 잘라서 보낸다.
     */
    private fun exploredSegmentIds(maxCount: Int): List<Long> =
        database.exploredSegmentQueries
            .selectCoveredSegmentIds(SegmentCoverageMatcher.DEFAULT_SEGMENT_COVERAGE)
            .executeAsList()
            .take(maxCount)

    /** 서버가 `newRoadM`을 줬으면 그대로 쓰고, 안 줬으면 로컬 `explored_segment`로 추정한다. */
    fun newRoadMeters(route: RouteDto): Double =
        route.newRoadM?.toDouble() ?: NewRoadEstimator.newRoadMeters(
            totalDistanceM = route.distanceM.toDouble(),
            exploredLengthM = exploredLengthFor(route.segmentIds),
        )

    /** 빈 목록이면 쿼리가 `IN ()`이 돼서 SQLite 문법 오류가 나므로 먼저 거른다. */
    private fun exploredLengthFor(segmentIds: List<Long>): Double =
        if (segmentIds.isEmpty()) {
            0.0
        } else {
            database.exploredSegmentQueries.exploredLengthForSegments(segmentIds).executeAsOne()
        }

    /**
     * `docs/local-db/local-db-schema-design.md` §3.4 `course` 테이블에 추천 응답을 그대로 저장하고,
     * 저장한 코스 ID를 돌려준다(썸네일 미리 만들기에 쓴다). 경로가 비어 있으면 null.
     *
     * [newRoadM]은 [newRoadMeters]가 정한 값 — "내 코스" 탭 카드가 코스 상세와 같은 숫자를 보여주도록
     * 저장할 때도 같은 값을 쓴다.
     */
    fun saveCourse(route: RouteDto, courseName: String, newRoadM: Double): String? {
        val start = route.path.firstOrNull() ?: return null
        val courseId = NSUUID().UUIDString

        database.courseQueries.insertCourse(
            id = courseId,
            source_track_id = null,
            name = courseName,
            distance_meters = route.distanceM.toDouble(),
            // [lng, lat] 순서가 이미 coordinates 컬럼이 기대하는 "[[lon,lat],…]"와 같다.
            coordinates = encodeDoubleMatrix(route.path),
            start_lat = start[1],
            start_lon = start[0],
            place_name = null,
            created_at_epoch_ms = (NSDate().timeIntervalSince1970 * 1000).toLong(),
            route_type = route.type,
            duration_sec = route.durationSec.toLong(),
            elevation_gain_m = route.elevationGainM?.toLong(),
            elevation_profile = route.elevationProfile?.let { encodeDoubleMatrix(it) },
            new_road_m = newRoadM.roundToLong(),
            new_road_ratio = route.newRoadRatio
                ?: (newRoadM / route.distanceM).takeIf { route.distanceM > 0 },
            tags = json.encodeToString(ListSerializer(String.serializer()), route.tags),
            segment_ids = json.encodeToString(ListSerializer(Long.serializer()), route.segmentIds),
            milestones = json.encodeToString(ListSerializer(MilestoneDto.serializer()), route.milestones),
        )
        return courseId
    }

    /** 저장 직후 "내 코스" 탭 썸네일을 미리 만들 때 쓰는 요청. */
    fun thumbnailRequest(courseId: String, path: List<LatLon>): SnapshotRequest? =
        IosSnapshot.request(path, courseId, SnapshotVariant.THUMBNAIL)

    /** `RouteDto.path`(GeoJSON `[lng, lat]`)를 지도에 그릴 좌표로. */
    fun pathOf(route: RouteDto): List<LatLon> =
        route.path.mapNotNull { pair -> pair.takeIf { it.size >= 2 }?.let { LatLon(lat = it[1], lon = it[0]) } }

    /** `RouteDto.elevationProfile`을 `[누적거리m, 고도m]` 쌍 목록으로. */
    fun elevationProfileOf(route: RouteDto): List<ElevationPoint> =
        route.elevationProfile?.mapNotNull { point ->
            point.takeIf { it.size >= 2 }?.let { ElevationPoint(cumulativeDistanceM = it[0], elevationM = it[1]) }
        } ?: emptyList()

    private fun encodeDoubleMatrix(values: List<List<Double>>): String =
        json.encodeToString(ListSerializer(ListSerializer(Double.serializer())), values)
}

/** 고도 그래프 한 점 — 출발부터 누적 거리(m)와 그 지점 고도(m). */
data class ElevationPoint(val cumulativeDistanceM: Double, val elevationM: Double)
