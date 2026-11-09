package com.swyp.runveal.tabrecord.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabrecord.RecordScreen
import com.swyp.runveal.tabrecord.RecordingScreen

fun NavGraphBuilder.recordGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.HomeTab.Record> {
        RecordScreen()
    }
}

fun NavGraphBuilder.recordingOverlay(onClose: () -> Unit) {
    composable<RunVealRoute.HomeTab.Recording> {
        RecordingScreen(onClose = onClose, modifier = Modifier.fillMaxSize())
    }
}
