package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.designsystem.component.list.RunvealRouteStepType
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.ui.mvi.UiState

/** [RunvealRouteStep][com.sloth.runveal.designsystem.component.list.RunvealRouteStep] 한 줄. */
data class RouteStepUiModel(
    val place: String,
    val note: String?,
    val distanceLabel: String,
    val type: RunvealRouteStepType,
)

/** 고도 그래프 한 점 — 출발부터 누적 거리(m)와 그 지점 고도(m). */
data class ElevationPointUiModel(val cumulativeDistanceM: Double, val elevationM: Double)

sealed interface CourseDetailState : UiState {
    data object Loading : CourseDetailState

    /** [CourseDetailIntent.Load]로 받은 routeId를 홀더에서 못 찾았을 때 — 새로고침 등으로 추천
     * 결과가 이미 사라진 경우. */
    data object NotFound : CourseDetailState

    data class Ready(
        /** "코스 A", "코스 B" ... 라벨의 글자 부분. 전체 문구는 화면에서 string resource로 조합한다.
         * 저장된 코스로 들어왔을 땐 쓰지 않는다 — [savedCourseName]이 있으면 그게 제목이다. */
        val courseLabel: String,
        /** "내 코스"에서 들어왔을 때 저장해 둔 코스 이름(이름 수정으로 바뀐 이름 포함). 추천 결과에서
         * 들어왔으면 null — 그때는 [courseLabel]로 "코스 A"처럼 조합한다. */
        val savedCourseName: String? = null,
        val distanceValue: String,
        val distanceUnit: String,
        val timeValue: String,
        val timeUnit: String,
        val elevationValue: String,
        val elevationUnit: String,
        val newRoadValue: String,
        val newRoadUnit: String,
        val elevationProfile: List<ElevationPointUiModel>,
        val routeSteps: List<RouteStepUiModel>,
        /** `RouteDto.path`를 지도에 그릴 수 있게 변환한 좌표. */
        val path: List<LatLon>,
        val isLoop: Boolean,
        /** [CourseDetailIntent.SaveCourse]로 로컬 `course` 테이블에 저장했는지 — 중복 저장 방지 + 버튼 문구에 쓴다. */
        val isSaved: Boolean = false,
    ) : CourseDetailState
}
