package com.swyp.runveal.tabcourse

import com.swyp.runveal.data.local.DatabaseDriverFactory
import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.db.SelectAllCourses
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.snapshot.SnapshotVariant
import com.swyp.runveal.map.snapshot.toSnapshotRequest
import com.swyp.runveal.tabcourse.contract.CourseListIntent
import com.swyp.runveal.tabcourse.contract.CourseListState
import com.swyp.runveal.tabcourse.contract.SavedCourseUiModel
import com.swyp.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** 탭 전환은 `restoreState`로 화면만 다시 구성하고 이 ViewModel 자체는 살아있으므로, `init`에서 한 번만
 * 불러오면 탭을 나갔다 온 사이 새로 저장한 코스가 안 보인다 — 화면이 들어올 때마다 [CourseListIntent.Load]를
 * 보내 매번 다시 불러오게 한다. Effect 타입 인자는 [Nothing]. */
class CourseListViewModel : MviViewModel<CourseListIntent, CourseListState, Nothing>(initialState = CourseListState.Loading) {
    private val database = RunvealDatabase(DatabaseDriverFactory().createDriver())
    private val json = Json

    override suspend fun handleIntent(intent: CourseListIntent) {
        when (intent) {
            CourseListIntent.Load -> reload()
        }
    }

    private suspend fun reload() {
        val courses = withContext(Dispatchers.IO) {
            database.courseQueries.selectAllCourses().executeAsList().map { it.toUiModel() }
        }
        setState { CourseListState.Ready(courses) }
    }

    private fun SelectAllCourses.toUiModel(): SavedCourseUiModel {
        val path = decodeCoordinates(coordinates)
        return SavedCourseUiModel(
            id = id,
            title = name,
            distanceLabel = "%.1fkm".format(distance_meters / 1000.0),
            durationLabel = duration_sec?.let { "${it / 60}분" } ?: "",
            tagLabel = when (route_type) {
                "LOOP" -> "순환"
                "ONE_WAY" -> "편도"
                else -> ""
            },
            newRoadLabel = new_road_m?.let { "새 길 %.1fkm".format(it / 1000.0) } ?: "",
            notRunLabel = "아직 안달림",
            path = path,
            isLoop = route_type == "LOOP",
            snapshotRequest = path.toSnapshotRequest(ownerId = id, variant = SnapshotVariant.THUMBNAIL),
        )
    }

    /** `course.coordinates` 컬럼("[[lon,lat],…]", 코스 저장 시 인코딩한 형식)을 되돌린다 — GeoJSON
     * 좌표 순서라 [lng, lat]라서 LatLon은 반대 순서로 매핑한다. */
    private fun decodeCoordinates(raw: String): List<LatLon> =
        json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
            .map { LatLon(lat = it[1], lon = it[0]) }
}
