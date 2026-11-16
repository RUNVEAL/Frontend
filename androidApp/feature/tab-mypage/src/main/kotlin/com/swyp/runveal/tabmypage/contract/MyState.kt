package com.swyp.runveal.tabmypage.contract

import com.swyp.runveal.ui.mvi.UiState

/**
 * 마이페이지 화면 상태. 지금은 전부 자리표시자다 — 값을 채워 줄 기능들이 아직 다른 브랜치에 있거나
 * 구현 전이라, 이 화면은 UI만 먼저 올린다.
 *  - [nickname]: 회원 정보(`sharedLogic`의 auth)와 연결 전.
 */
data class MyState(
    val nickname: String = "",
    val exploredDongCount: Int = 0,
    val totalDistanceKm: Double = 0.0,
    /** 아직 구현 전인 항목을 눌렀을 때 띄우는 안내. 문구는 화면이 string resource에서 가져온다. */
    val showNotReadyToast: Boolean = false,
) : UiState {
    /** 누적 거리는 소수점 둘째 자리까지 — 시안의 "35.10" 표기. */
    val totalDistanceLabel: String
        get() {
            val rounded = (totalDistanceKm * 100).toLong()
            return "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}"
        }
}
