package com.sloth.runveal.tabmypage.contract

import com.sloth.runveal.ui.mvi.UiState

/** 마이페이지에서 띄우는 안내 토스트의 종류. 어떤 문구를 쓸지는 화면이 고른다. */
enum class MyToast {
    /** 아직 붙일 기능이 없는 항목을 눌렀을 때. */
    NOT_READY,
}

/**
 * 마이페이지 화면 상태. 지금은 전부 자리표시자다 — 값을 채워 줄 기능들이 아직 다른 브랜치에 있거나
 * 구현 전이라, 이 화면은 UI만 먼저 올린다.
 *  - [nickname]: 가입 때 정한 값. 아직 가입 전이면 비어 있고, 화면이 자리표시자를 쓴다.
 */
data class MyState(
    val nickname: String = "",
    val exploredDongCount: Int = 0,
    val totalDistanceKm: Double = 0.0,
    /** 띄워 둘 안내 토스트. 문구는 화면이 string resource에서 가져온다. */
    val toast: MyToast? = null,
    val showLogoutDialog: Boolean = false,
    /** 로그아웃 요청 중. 소셜 로그아웃이 네트워크를 타서 바로 안 끝나므로 그 사이 중복 탭을 막는다. */
    val isLoggingOut: Boolean = false,
) : UiState {
    /** 누적 거리는 소수점 둘째 자리까지 — 시안의 "35.10" 표기. */
    val totalDistanceLabel: String
        get() {
            val rounded = (totalDistanceKm * 100).toLong()
            return "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}"
        }
}
