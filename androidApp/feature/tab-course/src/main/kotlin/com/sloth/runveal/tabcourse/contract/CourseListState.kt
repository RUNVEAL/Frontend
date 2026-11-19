package com.sloth.runveal.tabcourse.contract

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiIntent
import com.sloth.runveal.ui.mvi.UiState

/** 탭을 나갔다 들어올 때마다(`restoreState`로 화면만 재구성되고 ViewModel은 그대로 살아있음) 목록을
 * 다시 불러오게 하는 의도 — 코스 상세 화면의 진입 시 `Load` 패턴과 같다. */
sealed interface CourseListIntent : UiIntent {
    data object Load : CourseListIntent

    /** 목록에서 코스를 골랐다 — 위 큰 카드가 그 코스로 바뀐다. */
    data class SelectCourse(val courseId: String) : CourseListIntent

    /** 더보기 메뉴에서 "삭제"를 골랐다 — 되돌릴 수 없는 일이라 바로 지우지 않고 한 번 더 묻는다. */
    data class DeleteCourseClicked(val courseId: String) : CourseListIntent

    data object DeleteConfirmed : CourseListIntent

    data object DeleteDismissed : CourseListIntent
}

/** 저장한 코스 목록의 한 항목. */
data class SavedCourseUiModel(
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

sealed interface CourseListState : UiState {
    data object Loading : CourseListState

    /**
     * [courses]는 `created_at_epoch_ms` 최신순 — 비어있으면 빈 상태 화면을 보여준다.
     *
     * [selectedCourseId]는 위 큰 카드에 띄울 코스. 목록에서 코스를 누르면 바뀐다. null이거나
     * 목록에 없는 id면(코스를 지운 경우 등) 맨 위 코스로 떨어진다 — [featuredCourse] 참고.
     */
    data class Ready(
        val courses: List<SavedCourseUiModel>,
        val selectedCourseId: String? = null,
        /** 삭제 확인 다이얼로그를 띄워 둔 코스 — null이면 다이얼로그가 떠 있지 않다. */
        val pendingDeleteCourseId: String? = null,
    ) : CourseListState {
        /** 큰 카드에 띄울 코스. 고른 게 없으면 가장 최근에 저장한 코스다. */
        val featuredCourse: SavedCourseUiModel?
            get() = courses.firstOrNull { it.id == selectedCourseId } ?: courses.firstOrNull()
    }
}
