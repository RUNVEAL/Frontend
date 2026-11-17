package com.swyp.runveal.tabhome.contract

import com.swyp.runveal.ui.mvi.UiState

/**
 * 홈 지도 오버레이 상태. 지금은 현재 위치의 행정동뿐이고, 줌인 뷰의 탐험률·거리는 아직 자리표시자다.
 */
data class HomeMapOverlayState(
    /** `GET /api/v1/dong`이 준 행정동 이름. 아직 못 받았으면 null — 화면이 대체 문구를 쓴다. */
    val dongName: String? = null,
    /** 정복률 집계와 매칭할 10자리 행정동 코드. 줌인 뷰의 탐험률을 붙일 때 쓴다. */
    val dongCode: String? = null,
) : UiState
