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
        @Serializable
        data object Home : HomeTab

        @Serializable
        data object Course : HomeTab

        @Serializable
        data object Record : HomeTab

        @Serializable
        data object My : HomeTab
    }
}
