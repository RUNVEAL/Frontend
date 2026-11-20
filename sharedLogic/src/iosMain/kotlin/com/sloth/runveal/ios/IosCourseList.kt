package com.sloth.runveal.ios

import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.map.snapshot.SnapshotVariant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** 저장한 코스 목록의 한 항목 — 화면에 그대로 찍히는 라벨까지 여기서 만든다. */
data class SavedCourseItem(
    val id: String,
    val title: String,
    val distanceLabel: String,
    val durationLabel: String,
    val tagLabel: String,
    val newRoadLabel: String,
    val notRunLabel: String,
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
                title = row.name,
                distanceLabel = formatKilometers(row.distance_meters, 1) + "km",
                durationLabel = row.duration_sec?.let { "${it / 60}분" } ?: "",
                tagLabel = when (row.route_type) {
                    "LOOP" -> "순환"
                    "ONE_WAY" -> "편도"
                    else -> ""
                },
                newRoadLabel = row.new_road_m?.let { "새 길 " + formatKilometers(it.toDouble(), 1) + "km" } ?: "",
                notRunLabel = "아직 안달림",
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

    /** `course.coordinates` 컬럼("[[lon,lat],…]")을 되돌린다 — GeoJSON 좌표 순서라 반대로 매핑한다. */
    private fun decodeCoordinates(raw: String): List<LatLon> =
        try {
            json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
                .mapNotNull { pair -> pair.takeIf { it.size >= 2 }?.let { LatLon(lat = it[1], lon = it[0]) } }
        } catch (_: Exception) {
            emptyList()
        }
}
