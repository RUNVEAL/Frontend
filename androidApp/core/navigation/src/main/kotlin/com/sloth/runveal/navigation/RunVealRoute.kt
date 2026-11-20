package com.sloth.runveal.navigation

import kotlinx.serialization.Serializable

sealed interface RunVealRoute : Route {

    sealed interface Main : RunVealRoute {
        @Serializable
        data object Splash : Main

        /** 첫 실행에 한 번 보여주는 온보딩(3단계). 스플래시가 "아직 안 봤다"를 보고 여기로 보낸다. */
        @Serializable
        data object Onboarding : Main
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

        /** 기록 탭 목록에서 한 건을 눌러 들어가는 러닝 기록 상세. 값은 [trackId]로만 넘기고 화면이
         * 로컬 DB에서 다시 읽는다 — 경로·탐험률 같은 큰 값을 라우트에 실을 수 없다. */
        @Serializable
        data class RecordDetail(val trackId: String) : HomeTab

        @Serializable
        data object My : HomeTab

        /** MY 탭에서 들어가는 약관 및 정책 목록. 탭바를 숨기고 전체 화면으로 띄운다. */
        @Serializable
        data object Terms : HomeTab

        /** MY 탭에서 들어가는 Health Connect 기록 불러오기 안내. 탭바를 숨기고 전체 화면으로 띄운다. */
        @Serializable
        data object ImportRecords : HomeTab

        /** 기록 불러오기 진행 화면 — Health Connect에서 가져오는 동안 보인다. */
        @Serializable
        data object ImportRecordsProgress : HomeTab

        /** 기록 불러오기 결과. 값은 라우트가 아니라 `ImportResultHolder`에서 읽는다 — 경로·동
         * 목록은 라우트 인자로 싣기 어렵다(러닝 결과 화면이 `TrackResultHolder`를 쓰는 방식). */
        @Serializable
        data object ImportRecordsResult : HomeTab

        /** 약관 한 건의 본문. [item]은 어떤 약관인지(`TermsPolicyItem`의 이름) — 상단 제목과 본문을
         * 고르는 데 쓴다. 문서 자체를 라우트로 넘기지 않는 건 본문이 길고 서버에서 받아올 값이라서다. */
        @Serializable
        data class TermsDetail(val item: String) : HomeTab
    }
}
