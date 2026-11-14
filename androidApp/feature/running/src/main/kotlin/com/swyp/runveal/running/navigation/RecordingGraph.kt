package com.swyp.runveal.running.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.running.RecordingScreen

/** [onDiscarded]는 너무 짧아서 저장하지 않은 러닝 — 결과 화면 없이 홈으로 돌아가는 경로다. */
fun NavGraphBuilder.recordingOverlay(onFinished: () -> Unit, onDiscarded: () -> Unit) {
    composable<RunVealRoute.HomeTab.Recording> {
        RecordingScreen(onClose = onFinished, onDiscarded = onDiscarded, modifier = Modifier.fillMaxSize())
    }
}
