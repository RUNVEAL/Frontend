package com.sloth.runveal.tabmypage.contract

import com.sloth.runveal.ui.mvi.UiEffect

/**
 * 진행 화면을 떠날 때. 화면 이동을 상태가 아니라 Effect로 보내는 건
 * [com.sloth.runveal.external.ImportProgressHolder]가 전역 싱글톤이라서다 — 지난 실행의
 * `Finished`가 남아 있어 상태로는 "이번 실행이 끝났다"를 구분할 수 없다.
 */
sealed interface ImportRecordsProgressEffect : UiEffect {
    /** 가져오기가 끝났다 — 결과 화면으로 넘긴다. 집계·경로는 가져오기가
     * [com.sloth.runveal.external.ImportResultHolder]에 올려 둔 걸 결과 화면이 읽는다. */
    data object Finished : ImportRecordsProgressEffect

    /**
     * 시작조차 못 했다(권한이 중간에 끊겼거나 Health Connect를 쓸 수 없는 상태). 보여 줄 결과가
     * 없어서 결과 화면을 거치지 않고 그냥 닫는다 — 안내 화면이 누르기 전에 미리 걸러 주므로
     * 여기까지 오는 건 드문 경우다.
     */
    data object Unavailable : ImportRecordsProgressEffect
}
