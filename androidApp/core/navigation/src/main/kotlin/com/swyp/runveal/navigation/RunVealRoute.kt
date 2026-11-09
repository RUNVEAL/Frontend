package com.swyp.runveal.navigation

import kotlinx.serialization.Serializable

sealed interface RunVealRoute : Route {

    sealed interface Main : RunVealRoute {
        @Serializable
        data object Splash : Main
    }

    sealed interface Home : RunVealRoute {
        @Serializable
        data object Main : Home
    }

    sealed interface HomeTab : RunVealRoute {
        /** 홈 탭 그래프 시작점. 지도 위 흐름([HomeMap], [CourseRecommend])을 품는 중첩 그래프. */
        @Serializable
        data object Home : HomeTab

        /** 홈 탭의 기본 화면 — 지도 오버레이(줌 레벨별로 바뀜). */
        @Serializable
        data object HomeMap : HomeTab

        /** 지도를 유지한 채 탭바를 숨기고 올라가는 코스 추천 흐름(현재는 뼈대만). */
        @Serializable
        data object CourseRecommend : HomeTab

        /** 지도를 유지한 채 탭바를 숨기고 올라가는 러닝 기록 시작/종료 흐름. */
        @Serializable
        data object Recording : HomeTab

        @Serializable
        data object Course : HomeTab

        @Serializable
        data object Record : HomeTab

        @Serializable
        data object My : HomeTab
    }
}
