package com.swyp.runveal.tabrecord.contract

import com.swyp.runveal.ui.mvi.UiState

/** 기록 목록 한 줄 — 전부 화면에 그대로 찍히는 라벨이다(포맷은 [RecordViewModel][com.swyp.runveal.tabrecord.RecordViewModel]). */
data class RecordItem(
    val trackId: String,
    val dateLabel: String,
    val courseTitle: String,
    val distanceLabel: String,
    val durationLabel: String,
    val paceLabel: String,
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
) : UiState
