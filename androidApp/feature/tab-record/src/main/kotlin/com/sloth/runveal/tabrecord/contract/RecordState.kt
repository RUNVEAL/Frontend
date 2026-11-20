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
    /** "새 길 +1.30km" — 이번 러닝에서 처음 밟은 거리. 처음 간 길이 없으면 null이라 줄이 빠진다
     * (시안 `List/List Item/Record`의 Show New Road). */
    val newRoadLabel: String? = null,
    /** 카드 왼쪽 지도 썸네일. 매칭 경로가 없으면(매칭 실패/점 부족) null이라 플레이스홀더만 보인다. */
    val thumbnailRequest: SnapshotRequest? = null,
)

/**
 * 기록 탭 상태. 첫 DB 조회가 끝나기 전에는 [isLoading] — 이걸 안 두면 기록이 있는 사용자에게도
 * 빈 상태가 한 프레임 깜빡인다. 월 선택 UI가 붙기 전까지 기간은 항상 "이번 달"이다.
 */
data class RecordState(
    val monthLabel: String = "",
    /** 지금 보고 있는 달 — 월 선택 패널이 어디에 표시를 둘지 정하는 데 쓴다. */
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    /** 요약 줄 앞머리("이번 달" 또는 "10월") — 보고 있는 달이 이번 달이 아닐 수 있다. */
    val periodLabel: String = "",
    val runCountLabel: String = "0회",
    val totalDistanceValue: String = "0.00",
    val newRoadValue: String = "+0.00",
    /** 이번 달에 처음 간 길이 있는지 — 요약 줄의 금색 강조를 켤지 정한다(시안: 0이면 강조 없음). */
    val hasNewRoad: Boolean = false,
    val records: List<RecordItem> = emptyList(),
    val isLoading: Boolean = true,
) : UiState
