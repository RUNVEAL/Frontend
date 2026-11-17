package com.sloth.runveal.tabrecord.contract

import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.ui.mvi.UiState

/** 기록 목록 한 줄 — 전부 화면에 그대로 찍히는 라벨이다(포맷은 [RecordViewModel][com.sloth.runveal.tabrecord.RecordViewModel]). */
data class RecordItem(
    val trackId: String,
    val dateLabel: String,
    val courseTitle: String,
    val distanceLabel: String,
    val durationLabel: String,
    val paceLabel: String,
    /** 카드 왼쪽 지도 썸네일. 매칭 경로가 없으면(매칭 실패/점 부족) null이라 플레이스홀더만 보인다. */
    val thumbnailRequest: SnapshotRequest? = null,
)

/**
 * 기록 탭 상태. 첫 DB 조회가 끝나기 전에는 [isLoading] — 이걸 안 두면 기록이 있는 사용자에게도
 * 빈 상태가 한 프레임 깜빡인다. 월 선택 UI가 붙기 전까지 기간은 항상 "이번 달"이다.
 */
data class RecordState(
    val monthLabel: String = "",
    val runCountLabel: String = "0회",
    val totalDistanceValue: String = "0.00",
    val newRoadValue: String = "+0.00",
    val records: List<RecordItem> = emptyList(),
    val isLoading: Boolean = true,
    /** 삭제 확인 다이얼로그를 띄울 기록. null이면 다이얼로그가 떠 있지 않다. 되돌릴 수 없는
     * 동작이라 길게 누른 즉시 지우지 않고 한 번 더 묻는다. */
    val deleteTargetTrackId: String? = null,
) : UiState
