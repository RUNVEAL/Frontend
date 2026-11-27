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
    /** 더보기 메뉴에서 "삭제"를 골랐다 — 되돌릴 수 없는 일이라 바로 지우지 않고 한 번 더 묻는다. */
    data class DeleteCourseClicked(val courseId: String) : CourseListIntent

    data object DeleteConfirmed : CourseListIntent

    data object DeleteDismissed : CourseListIntent

    /** 더보기 메뉴에서 "이름 수정"을 골랐다 — 지금 이름을 채운 입력 시트를 띄운다. */
    data class RenameCourseClicked(val courseId: String) : CourseListIntent

    /** 입력 시트에서 "저장"을 눌렀다. 앞뒤 공백은 ViewModel이 다듬고, 빈 이름은 무시한다. */
    data class RenameConfirmed(val name: String) : CourseListIntent

    data object RenameDismissed : CourseListIntent
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
    /** 이 코스로 달려서 기록이 저장된 적이 있는지 — "다음에 달려볼 코스" 카드가 안 달린 것만 고른다. */
    val hasRun: Boolean,
    val path: List<LatLon>,
    /** 순환 코스면 출발·도착이 같은 자리 — 기록 화면이 도착 핀을 생략하는 데 쓴다. */
    val isLoop: Boolean,
    val snapshotRequest: SnapshotRequest?,
)

sealed interface CourseListState : UiState {
    data object Loading : CourseListState

    /** [courses]는 `created_at_epoch_ms` 최신순 — 비어있으면 빈 상태 화면을 보여준다. */
    data class Ready(
        val courses: List<SavedCourseUiModel>,
        /** 삭제 확인 다이얼로그를 띄워 둔 코스 — null이면 다이얼로그가 떠 있지 않다. */
        val pendingDeleteCourseId: String? = null,
        /** 이름 수정 시트를 띄워 둔 코스 — null이면 시트가 떠 있지 않다. */
        val pendingRenameCourseId: String? = null,
    ) : CourseListState {
        /**
         * "다음에 달려볼 코스" 카드에 띄울 코스 — **아직 안 달린** 코스 중 가장 최근에 저장한 것.
         *
         * [courses]가 이미 저장 최신순이라 앞에서부터 처음 걸리는 안 달린 코스가 그 코스다.
         * 전부 달렸으면 null — 그때는 화면이 새 코스를 만들라고 권하는 카드로 바꿔 띄운다.
         */
        val featuredCourse: SavedCourseUiModel?
            get() = courses.firstOrNull { !it.hasRun }

        /** 저장한 코스는 있는데 전부 달린 상태 — 카드 자리에 "코스 만들기" 유도를 띄운다. */
        val hasRunAllCourses: Boolean
            get() = courses.isNotEmpty() && courses.all { it.hasRun }

        /** 이름 수정 시트에 지금 이름을 채워 넣을 코스. */
        val pendingRenameCourse: SavedCourseUiModel?
            get() = courses.firstOrNull { it.id == pendingRenameCourseId }
    }
}
