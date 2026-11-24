package com.sloth.runveal.tabhome.contract

import com.sloth.runveal.ui.mvi.UiState

/**
 * 홈 지도 오버레이 상태 — 행정동이 **둘** 들어 있다.
 *
 * - [dongName]/[dongCode]: 지금 **내가 서 있는** 동. 줌인 뷰가 쓴다.
 * - [centerDongName]/[centerDongCode]: 지금 **지도 가운데 보이는** 동. 줌아웃 뷰가 쓴다.
 *
 * 지도를 끌지 않았으면 둘이 같지만, 끌고 나면 갈라진다 — 그래서 한 쌍으로 합치지 않는다.
 */
data class HomeMapOverlayState(
    /** 지금 서 있는 행정동 — 예: "부성1동". 아직 못 받았으면 null — 화면이 대체 문구를 쓴다. */
    val dongName: String? = null,
    /** 정복률 집계와 매칭할 10자리 행정동 코드. 줌인 뷰의 탐험률을 붙일 때 쓴다. */
    val dongCode: String? = null,
    /** 지도 가운데 보이는 행정동 — 카메라가 멈출 때마다 다시 받는다(줌아웃 뷰 전용). */
    val centerDongName: String? = null,
    /** [centerDongName]의 10자리 행정동 코드 — 요약 카드가 이 동의 탐험률을 꺼내는 키다. */
    val centerDongCode: String? = null,
) : UiState
