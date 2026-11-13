package com.swyp.runveal.running.navigation

import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.running.ResultScreen

fun NavGraphBuilder.resultOverlay(onClose: () -> Unit) {
    composable<RunVealRoute.HomeTab.Result>(
        // 뒤로가기/닫기로 이 화면이 사라질 때는 기본 슬라이드 대신 페이드아웃만 — 슬라이드가 어색해서.
        popExitTransition = { fadeOut() },
    ) {
        ResultScreen(onClose = onClose, modifier = Modifier.fillMaxSize())
    }
}
