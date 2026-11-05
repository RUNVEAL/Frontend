package com.swyp.runveal.tabhome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swyp.runveal.core.map.RunvealMap
import com.swyp.runveal.permission.RequireLocationPermission
import com.swyp.runveal.tabhome.component.HomeMapOverlay

@Composable
fun HomeMapScreen(modifier: Modifier = Modifier) {
    RequireLocationPermission(modifier = modifier) {
        Box(modifier = modifier.fillMaxSize()) {
            RunvealMap(modifier = Modifier.fillMaxSize())
            HomeMapOverlay(locationName = "현재 위치", modifier = Modifier.fillMaxSize())
        }
    }
}
