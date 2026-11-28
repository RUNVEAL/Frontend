package com.sloth.runveal.ios

import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlin.math.roundToInt

/** 저장한 코스 목록의 한 항목 — 화면에 그대로 찍히는 라벨까지 여기서 만든다. */
data class SavedCourseItem(
    val id: String,
    val title: String,
    val distanceLabel: String,
    val durationLabel: String,
    val tagLabel: String,
    val newRoadLabel: String,
    val notRunLabel: String,
    /** 이 코스로 달려서 기록이 저장된 적이 있는지 — "다음에 달려볼 코스" 카드가 안 달린 것만 고른다. */
    val hasRun: Boolean,
    val path: List<LatLon>,
    /** 순환 코스면 출발·도착이 같은 자리 — 기록 화면이 도착 핀을 생략하는 데 쓴다. */
    val isLoop: Boolean,
    val snapshotRequest: SnapshotRequest?,
)

/**
 * "내 코스" 탭이 보여줄 저장한 코스 목록 — Android `CourseListViewModel`의 DB 조회·포맷 부분에 해당한다.
 *
 * SQLDelight가 만든 쿼리 타입을 Swift가 직접 다루지 않게 Kotlin에서 끝낸다.
 */
object IosCourseList {
    private val json = Json

    /** `created_at_epoch_ms` 최신순 — 비어 있으면 화면이 빈 상태를 보여준다. */
    fun load(): List<SavedCourseItem> =
        RunvealDatabaseProvider.database.courseQueries.selectAllCourses().executeAsList().map { row ->
            val path = decodeCoordinates(row.coordinates)
            SavedCourseItem(
                id = row.id,
                // 카드·목록 모두 한 줄짜리 자리라 길면 뒤를 줄인다(시안: 최대 10글자).
                title = row.name.take(MAX_TITLE_LENGTH),
                distanceLabel = formatKilometers(row.distance_meters, 1) + "km",
                // 서버가 준 duration_sec을 쓰지 않는다 — 추천 코스에만 있고 기록을 승격한 코스엔
                // 없어서 어떤 코스는 시간이 비어 보였다. 거리에 기본 페이스를 곱해 **모든 코스에 같은
                // 기준**으로 낸다(시안: 6분 30초/km, 분 단위 반올림).
                durationLabel = "${(row.distance_meters / 1000.0 * DEFAULT_PACE_SEC_PER_KM / 60).roundToInt()}분",
                tagLabel = when (row.route_type) {
                    "LOOP" -> "순환"
                    "ONE_WAY" -> "편도"
                    else -> ""
                },
                newRoadLabel = row.new_road_m?.let { "새 길 " + formatKilometers(it.toDouble(), 1) + "km" } ?: "",
                notRunLabel = if (row.last_run_at_epoch_ms == null) "아직 안 달림" else "",
                hasRun = row.last_run_at_epoch_ms != null,
                path = path,
                isLoop = row.route_type == "LOOP",
                // 코스 썸네일엔 출발·도착 점을 같이 찍는다 — 순환 코스는 도착이 곧 출발이라
                // 출발 점만 남는다(`toSnapshotRequest`가 같은 자리면 도착을 뺀다).
                snapshotRequest = IosSnapshot.request(
                    points = path,
                    ownerId = row.id,
                    variant = SnapshotVariant.THUMBNAIL,
                    withStartMarker = true,
                    withEndMarker = row.route_type != "LOOP",
                ),
            )
        }

    /**
     * 코스 이름을 바꾼다. 공백만 들어오면 아무것도 하지 않는다 — 화면의 "저장" 버튼도 같은 조건으로
     * 꺼져 있다(Android `CourseListViewModel.renameCourse`와 같다).
     *
     * 썸네일은 경로로 캐시되니 이름이 바뀌어도 그대로 쓴다.
     */
    fun rename(courseId: String, name: String) {
        val newName = name.trim()
        if (newName.isEmpty()) return
        RunvealDatabaseProvider.database.courseQueries.renameCourse(name = newName, id = courseId)
    }

    /**
     * 이 코스로 달린 기록이 **저장됐을 때** 마지막 달린 시각을 적는다 — "다음에 달려볼 코스" 카드가
     * 아직 안 달린 코스만 고르는 데 쓴다(Android `RecordingViewModel.markSelectedCourseRun`).
     *
     * 100m·1분을 못 채워 버려지는 러닝은 부르지 않는다 — 그런 것까지 세면 "달렸다"는 표시가 실제
     * 기록과 어긋난다.
     */
    fun markRun(courseId: String) {
        RunvealDatabaseProvider.database.courseQueries.markCourseRun(
            last_run_at_epoch_ms = (NSDate().timeIntervalSince1970 * 1000).toLong(),
            id = courseId,
        )
    }

    /** 코스 행을 지운다. 썸네일 캐시 정리는 호출하는 쪽(Swift `CourseSnapshotRenderer`)이 맡는다. */
    fun delete(courseId: String) {
        RunvealDatabaseProvider.database.courseQueries.deleteCourse(courseId)
    }

    /** `course.coordinates` 컬럼("[[lon,lat],…]")을 되돌린다 — GeoJSON 좌표 순서라 반대로 매핑한다. */
    private fun decodeCoordinates(raw: String): List<LatLon> =
        try {
            json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
                .mapNotNull { pair -> pair.takeIf { it.size >= 2 }?.let { LatLon(lat = it[1], lon = it[0]) } }
        } catch (_: Exception) {
            emptyList()
        }
}

/** 코스명 표시 길이 상한(시안) — 넘으면 뒤를 자른다. */
private const val MAX_TITLE_LENGTH = 10

/**
 * 예상 시간을 낼 때 쓰는 기본 페이스(초/km, 시안 6분 30초). 코스마다 실제 페이스를 알 수 없어
 * 모두 같은 기준으로 환산한다 — Android `CourseListViewModel`과 같은 값이다.
 */
private const val DEFAULT_PACE_SEC_PER_KM = 390.0
