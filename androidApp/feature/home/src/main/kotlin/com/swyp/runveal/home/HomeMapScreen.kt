package com.swyp.runveal.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swyp.runveal.core.map.RunvealMap
import com.swyp.runveal.permission.RequireLocationPermission

@Composable
internal fun HomeMapScreen(modifier: Modifier = Modifier) {
    RequireLocationPermission(modifier = modifier) {
        RunvealMap(modifier = modifier.fillMaxSize())
    }
}
