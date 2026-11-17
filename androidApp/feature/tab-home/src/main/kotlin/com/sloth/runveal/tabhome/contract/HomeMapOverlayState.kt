package com.sloth.runveal.tabhome.contract

import com.sloth.runveal.ui.mvi.UiState

/**
 * 홈 지도 오버레이 상태. 지금은 현재 위치의 행정동뿐이고, 줌인 뷰의 탐험률·거리는 아직 자리표시자다.
 */
data class HomeMapOverlayState(
    /** 지금 서 있는 행정동 — 예: "부성1동". 줌인·줌아웃 둘 다 시군구 없이 이 이름만 보여준다.
     * 아직 못 받았으면 null — 화면이 대체 문구를 쓴다. */
    val dongName: String? = null,
    /** 정복률 집계와 매칭할 10자리 행정동 코드. 줌인 뷰의 탐험률을 붙일 때 쓴다. */
    val dongCode: String? = null,
) : UiState
