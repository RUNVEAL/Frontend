package com.swyp.runveal.navigation

import kotlinx.serialization.Serializable

/**
 * 앱 전체의 navigation 목적지를 모아두는 sealed 계층.
 * feature별로 하위 sealed interface를 두고, 실제 destination은 @Serializable data object/class로 정의한다.
 */
sealed interface RunVealRoute : Route {

    sealed interface Main : RunVealRoute {
        @Serializable
        data object Splash : Main
    }

    sealed interface Home : RunVealRoute {
        @Serializable
        data object Main : Home
    }
}
