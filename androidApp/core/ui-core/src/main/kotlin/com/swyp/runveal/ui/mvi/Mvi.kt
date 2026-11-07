package com.swyp.runveal.ui.mvi

/** MVI 화면 상태 — ViewModel이 한 번에 들고 있는 값 전부. */
interface UiState

/** MVI 사용자 의도 — 화면에서 ViewModel로 보내는 액션. */
interface UiIntent

/** MVI 이펙트 — 한 번만 소비되는 부수효과(네비게이션, 토스트 등). 효과가 없는 화면은 [Nothing]을 쓰면 된다. */
interface UiEffect
