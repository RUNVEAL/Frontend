package com.swyp.runveal.running.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.running.ResultScreen

fun NavGraphBuilder.resultOverlay(onClose: () -> Unit) {
    composable<RunVealRoute.HomeTab.Result> {
        ResultScreen(onClose = onClose, modifier = Modifier.fillMaxSize())
    }
}
