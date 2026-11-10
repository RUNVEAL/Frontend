package com.swyp.runveal.running.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.running.RecordingScreen

fun NavGraphBuilder.recordingOverlay(onFinished: () -> Unit) {
    composable<RunVealRoute.HomeTab.Recording> {
        RecordingScreen(onClose = onFinished, modifier = Modifier.fillMaxSize())
    }
}
