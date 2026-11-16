package com.swyp.runveal.navigation

import kotlinx.serialization.Serializable

sealed interface RunVealRoute : Route {

    sealed interface Main : RunVealRoute {
        @Serializable
        data object Splash : Main
    }

    /** 로그인 + 신규 가입 흐름. 신규면 [SignIn] → [Terms] → [Nickname]을 거쳐 홈으로 간다. */
    sealed interface Sign : RunVealRoute {
        /** 소셜 로그인 화면. 스플래시가 로컬 토큰을 못 찾았을 때 여기로 보낸다. */
        @Serializable
        data object SignIn : Sign

        /** 약관 동의 — 신규 사용자만 거친다. 가입 도중 앱이 끊기면 스플래시가 다시 여기로 보낸다. */
        @Serializable
        data object Terms : Sign

        /** 닉네임 입력. "가입 완료"를 누르면 가입이 확정되고 홈으로 간다. */
        @Serializable
        data object Nickname : Sign
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

        /** 코스 추천 결과 화면 — `POST /api/v1/routes` 응답(0~3개)을 보여준다. */
        @Serializable
        data object CourseRecommendResult : HomeTab

        /** 코스 추천 결과에서 카드를 눌러 들어가는 코스 상세 — [routeId]로 어느 코스인지 들고 있는다
         * ([CourseRecommendResultHolder]가 이미 들고 있는 응답에서 다시 찾아 쓴다). */
        @Serializable
        data class CourseDetail(val routeId: String) : HomeTab

        /** 지도를 유지한 채 탭바를 숨기고 올라가는 러닝 기록 시작/종료 흐름. */
        @Serializable
        data object Recording : HomeTab

        /** 기록 종료 후 러닝 결과 화면 — 배경이 불투명이라 지도는 안 보이지만, 탭바 숨김 흐름은
         * [Recording]/[CourseRecommend]와 같은 중첩 그래프를 그대로 쓴다. */
        @Serializable
        data object Result : HomeTab

        @Serializable
        data object Course : HomeTab

        @Serializable
        data object Record : HomeTab

        @Serializable
        data object My : HomeTab
    }
}
