package com.swyp.runveal.tabhome.contract

import com.swyp.runveal.ui.mvi.UiState

/**
 * 홈 지도 오버레이 상태. 지금은 현재 위치의 행정동뿐이고, 줌인 뷰의 탐험률·거리는 아직 자리표시자다.
 */
data class HomeMapOverlayState(
    /** 시군구까지 붙은 이름 — 예: "천안시서북구 부성1동". 지도 상단 "현재 위치" 자리에 쓴다.
     * 아직 못 받았으면 null — 화면이 대체 문구를 쓴다. */
    val dongName: String? = null,
    /** 행정동만 — 예: "부성1동". 줌인 헤더처럼 "내 동네"가 바로 앞에 붙는 자리에 쓴다. */
    val dongOnly: String? = null,
    /** 정복률 집계와 매칭할 10자리 행정동 코드. 줌인 뷰의 탐험률을 붙일 때 쓴다. */
    val dongCode: String? = null,
) : UiState
