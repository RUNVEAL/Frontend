package com.sloth.runveal.tabcourse.contract

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiIntent
import com.sloth.runveal.ui.mvi.UiState

/** 탭을 나갔다 들어올 때마다(`restoreState`로 화면만 재구성되고 ViewModel은 그대로 살아있음) 목록을
 * 다시 불러오게 하는 의도 — 코스 상세 화면의 진입 시 `Load` 패턴과 같다. */
sealed interface CourseListIntent : UiIntent {
    data object Load : CourseListIntent
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

    /** [courses]는 `created_at_epoch_ms` 최신순 — 비어있으면 빈 상태 화면을 보여준다. */
    data class Ready(val courses: List<SavedCourseUiModel>) : CourseListState
}
