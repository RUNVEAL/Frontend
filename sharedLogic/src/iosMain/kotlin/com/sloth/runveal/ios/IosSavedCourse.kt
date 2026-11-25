package com.sloth.runveal.ios

import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.network.routes.MilestoneDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * "내 코스"에서 눌러 들어간 **이미 저장된** 코스 한 건 — 로컬 `course` 테이블에서 읽는다.
 *
 * 추천 결과에서 들어가는 코스 상세는 메모리에 있는 응답([CourseRecommendResultHolder])에서 찾지만,
 * 저장된 코스는 그 응답이 이미 사라진 뒤에도 열 수 있어야 해서 출처가 다르다. 화면은 같은 것을 쓴다
 * (Android `CourseDetailSource.Saved`와 같은 구조).
 *
 * 포맷된 문자열이 아니라 **값 그대로** 넘긴다 — 라벨 조립은 추천 쪽과 한곳(Swift
 * `CourseDetailViewModel`)에서 하게 두는 편이 두 경로가 어긋나지 않는다.
 */
data class SavedCourseDetail(
    /** 저장할 때 붙인 이름 — 제목에 그대로 쓴다("코스 {이름}"으로 조합하지 않는다). */
    val name: String,
    val distanceMeters: Double,
    /** 없으면 화면이 시간 칸을 비운다. */
    val durationMinutes: Int?,
    val elevationGainM: Int?,
    val newRoadMeters: Double,
    val elevationProfile: List<ElevationPoint>,
    /** 경로 정보 줄을 만드는 재료 — 추천 코스와 같은 [MilestoneDto]라 Swift 쪽 변환을 그대로 쓴다. */
    val milestones: List<MilestoneDto>,
    val path: List<LatLon>,
    val isLoop: Boolean,
)

object IosSavedCourse {
    private val json = Json { ignoreUnknownKeys = true }

    /** 못 찾으면 null — 화면은 "코스를 찾을 수 없음"을 보여준다. */
    fun detail(courseId: String): SavedCourseDetail? {
        val row = RunvealDatabaseProvider.database.courseQueries
            .selectCourseById(courseId)
            .executeAsOneOrNull() ?: return null

        return SavedCourseDetail(
            name = row.name,
            distanceMeters = row.distance_meters,
            durationMinutes = row.duration_sec?.let { (it / 60).toInt() },
            elevationGainM = row.elevation_gain_m?.toInt(),
            newRoadMeters = (row.new_road_m ?: 0L).toDouble(),
            elevationProfile = decodeElevationProfile(row.elevation_profile),
            milestones = decodeMilestones(row.milestones),
            path = decodeCoordinates(row.coordinates),
            isLoop = row.route_type == "LOOP",
        )
    }

    /** `course.coordinates` 컬럼("[[lon,lat],…]")을 되돌린다 — GeoJSON 좌표 순서라 반대로 매핑한다
     *  ([IosCourseList]와 같은 처리). */
    private fun decodeCoordinates(raw: String): List<LatLon> =
        decodeDoubleMatrix(raw).mapNotNull { pair ->
            pair.takeIf { it.size >= 2 }?.let { LatLon(lat = it[1], lon = it[0]) }
        }

    private fun decodeElevationProfile(raw: String?): List<ElevationPoint> =
        decodeDoubleMatrix(raw).mapNotNull { point ->
            point.takeIf { it.size >= 2 }?.let { ElevationPoint(cumulativeDistanceM = it[0], elevationM = it[1]) }
        }

    /** 저장할 때 `MilestoneDto.serializer()`로 넣은 그대로 되돌린다([IosCourseRecommend.saveCourse]). */
    private fun decodeMilestones(raw: String?): List<MilestoneDto> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(MilestoneDto.serializer()), raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeDoubleMatrix(raw: String?): List<List<Double>> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
