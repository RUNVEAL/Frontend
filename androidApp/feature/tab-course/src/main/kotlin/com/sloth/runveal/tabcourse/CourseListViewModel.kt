package com.sloth.runveal.tabcourse

import com.sloth.runveal.auth.AndroidAppContextHolder
import com.sloth.runveal.core.map.CourseSnapshotRenderer
import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.db.SelectAllCourses
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotVariant
import com.sloth.runveal.map.snapshot.toSnapshotRequest
import com.sloth.runveal.tabcourse.contract.CourseListIntent
import com.sloth.runveal.tabcourse.contract.CourseListState
import com.sloth.runveal.tabcourse.contract.SavedCourseUiModel
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** 탭 전환은 `restoreState`로 화면만 다시 구성하고 이 ViewModel 자체는 살아있으므로, `init`에서 한 번만
 * 불러오면 탭을 나갔다 온 사이 새로 저장한 코스가 안 보인다 — 화면이 들어올 때마다 [CourseListIntent.Load]를
 * 보내 매번 다시 불러오게 한다. Effect 타입 인자는 [Nothing]. */
class CourseListViewModel : MviViewModel<CourseListIntent, CourseListState, Nothing>(initialState = CourseListState.Loading) {
    private val database = RunvealDatabaseProvider.database
    private val json = Json

    /** 코스를 지울 때 그 썸네일 캐시도 같이 지운다([CourseSnapshotRenderer.delete]). */
    // 여기서는 캐시를 **지우는** 데만 써서 색이 실제로 쓰이지는 않지만, 렌더러를 만들려면 넘겨야 한다.
    private val snapshotRenderer = CourseSnapshotRenderer(context = AndroidAppContextHolder.context)

    override suspend fun handleIntent(intent: CourseListIntent) {
        when (intent) {
            CourseListIntent.Load -> reload()
            is CourseListIntent.DeleteCourseClicked ->
                setState {
                    (this as? CourseListState.Ready)?.copy(pendingDeleteCourseId = intent.courseId) ?: this
                }

            CourseListIntent.DeleteDismissed ->
                setState { (this as? CourseListState.Ready)?.copy(pendingDeleteCourseId = null) ?: this }

            CourseListIntent.DeleteConfirmed -> deleteCourse()

            is CourseListIntent.RenameCourseClicked ->
                setState {
                    (this as? CourseListState.Ready)?.copy(pendingRenameCourseId = intent.courseId) ?: this
                }

            CourseListIntent.RenameDismissed ->
                setState { (this as? CourseListState.Ready)?.copy(pendingRenameCourseId = null) ?: this }

            is CourseListIntent.RenameConfirmed -> renameCourse(intent.name)
        }
    }

    /** 입력 시트에서 "저장"을 누른 뒤 — 이름만 바꾸고 목록을 다시 읽는다(썸네일은 경로로 캐시되니
     * 이름이 바뀌어도 그대로 쓴다). 다시 읽으면서 [CourseListState.Ready]가 새로 만들어지므로 시트도
     * 같이 닫힌다. 공백만 들어오면 아무것도 하지 않는다 — 시트의 "저장" 버튼도 같은 조건으로 꺼진다. */
    private suspend fun renameCourse(name: String) {
        val courseId = (currentState as? CourseListState.Ready)?.pendingRenameCourseId ?: return
        val newName = name.trim()
        if (newName.isEmpty()) return
        withContext(Dispatchers.IO) { database.courseQueries.renameCourse(name = newName, id = courseId) }
        reload()
    }

    /** 확인 다이얼로그에서 "삭제"를 누른 뒤 — 코스 행과 그 썸네일 캐시를 지우고 목록을 다시 읽는다.
     * 다시 읽으면서 [CourseListState.Ready]가 새로 만들어지므로 다이얼로그도 같이 닫힌다. */
    private suspend fun deleteCourse() {
        val courseId = (currentState as? CourseListState.Ready)?.pendingDeleteCourseId ?: return
        withContext(Dispatchers.IO) {
            database.courseQueries.deleteCourse(courseId)
            snapshotRenderer.delete(courseId)
        }
        reload()
    }

    private suspend fun reload() {
        val courses = withContext(Dispatchers.IO) {
            database.courseQueries.selectAllCourses().executeAsList().map { it.toUiModel() }
        }
        setState { CourseListState.Ready(courses = courses) }
    }

    private fun SelectAllCourses.toUiModel(): SavedCourseUiModel {
        val path = decodeCoordinates(coordinates)
        return SavedCourseUiModel(
            id = id,
            // 카드·목록 모두 한 줄짜리 자리라 길면 뒤를 줄인다(시안: 최대 10글자).
            title = name.take(MAX_TITLE_LENGTH),
            distanceLabel = "%.1fkm".format(distance_meters / 1000.0),
            // 서버가 준 duration_sec을 쓰지 않는다 — 추천 코스에만 있고 기록을 승격한 코스엔 없어서
            // 어떤 코스는 시간이 비어 보였다. 거리에 기본 페이스를 곱해 **모든 코스에 같은 기준**으로
            // 낸다(시안: 6분 30초/km, 분 단위 반올림).
            durationLabel = "${(distance_meters / 1000.0 * DEFAULT_PACE_SEC_PER_KM / 60).roundToInt()}분",
            tagLabel = when (route_type) {
                "LOOP" -> "순환"
                "ONE_WAY" -> "편도"
                else -> ""
            },
            newRoadLabel = new_road_m?.let { "새 길 %.1fkm".format(it / 1000.0) } ?: "",
            notRunLabel = if (last_run_at_epoch_ms == null) "아직 안 달림" else "",
            hasRun = last_run_at_epoch_ms != null,
            path = path,
            isLoop = route_type == "LOOP",
            // 코스 썸네일엔 출발·도착 점을 같이 찍는다 — 카드가 그림 한가운데에 점을 얹던 걸 대신한다.
            // 순환 코스는 도착이 곧 출발이라 출발 점만 남는다.
            snapshotRequest = path.toSnapshotRequest(
                ownerId = id,
                variant = SnapshotVariant.THUMBNAIL,
                withStartMarker = true,
                withEndMarker = route_type != "LOOP",
            ),
        )
    }

    /** `course.coordinates` 컬럼("[[lon,lat],…]", 코스 저장 시 인코딩한 형식)을 되돌린다 — GeoJSON
     * 좌표 순서라 [lng, lat]라서 LatLon은 반대 순서로 매핑한다. */
    private fun decodeCoordinates(raw: String): List<LatLon> =
        json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
            .map { LatLon(lat = it[1], lon = it[0]) }
}

/** 코스명 표시 길이 상한(시안) — 넘으면 뒤를 자른다. */
private const val MAX_TITLE_LENGTH = 10

/** 예상 시간을 낼 때 쓰는 기본 페이스(초/km, 시안 6분 30초). 코스마다 실제 페이스를 알 수 없어
 * 모두 같은 기준으로 환산한다. */
private const val DEFAULT_PACE_SEC_PER_KM = 390.0
