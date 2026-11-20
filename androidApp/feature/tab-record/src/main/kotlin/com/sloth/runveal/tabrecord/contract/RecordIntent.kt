package com.sloth.runveal.tabrecord.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface RecordIntent : UiIntent {
    /**
     * 목록을 다시 읽는다 — 화면에 들어올 때마다 보낸다. 상세에서 기록을 지우고 돌아오면 ViewModel은
     * 그대로 살아 있어서(탭 스택) 다시 읽지 않으면 지운 기록이 목록에 남는다(내 코스 탭과 같은 처리).
     */
    data object Load : RecordIntent
}
